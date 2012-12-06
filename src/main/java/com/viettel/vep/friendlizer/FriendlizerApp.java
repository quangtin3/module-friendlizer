package com.viettel.vep.friendlizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Make selected module friendly with Viettel Enterprise Platform Tools
 *
 * @author quanghx2@viettel.com.vn
 */
public class FriendlizerApp {

    private static final String HOME_QUANGHX_MODULES = "/home/quanghx/modules/";
    private static final String VEP_TOOLS_MODULE_NAME = "com.viettel.vep.tools";
    private static final String NETBEANS_MODULE_FRIEND_KEY = "OpenIDE-Module-Friends";
    private static final int BUFFER_SIZE = 4096 * 2;
    private static final String META_INF_MANIFESTM_ENTRY = "META-INF/MANIFEST.MF";
    private static final String SUBFIX_VEP_TOOL_TEMPLATE_ZIP_FILE = "_vep_tool_template.zip";

    public static void main(String[] args) throws IOException {
        System.out.println("Hello World!");

        File folder = new File(HOME_QUANGHX_MODULES);
        String[] filenames = folder.list();

        for (String filename : filenames) {
            Log("Examining " + filename);
            if (filename.endsWith("jar") && (new File(HOME_QUANGHX_MODULES + filename)).isFile()) {
                patchingModuleJarFile(HOME_QUANGHX_MODULES + filename);
            }
        }

    }

    /**
     * Simple system log. Message will go on Terminal Output for now.
     *
     * @param message message your need to log
     */
    private static void Log(String message) {
        System.out.println(message);
    }

    /**
     * Replace file
     *
     * @param targetPath target path
     * @param newPath new file path
     */
    private static void replaceFile(String targetPath, String newPath) {
        File targetFile = new File(targetPath);
        File newFile = new File(newPath);

        if (targetFile.exists()
                && targetFile.isFile()
                && newFile.exists()
                && newFile.canRead()) {
            targetFile.deleteOnExit();            

            newFile.renameTo(new File(targetPath));
        } else {
            Log("Can't replace file " + targetPath + " with file " + newPath);
        }
    }

    /**
     * Patching module jar file procedure. For detail see:
     * http://javahowto.blogspot.com/2011/07/how-to-programmatically-copy-jar-files.html
     *
     * @param filename jar file to patch
     * @throws IOException error occur
     */
    private static void patchingModuleJarFile(String filename) throws IOException {
        JarFile jarfile = new JarFile(filename);

        Manifest manifest = jarfile.getManifest();
        Attributes att = manifest.getMainAttributes();

        if (updateManifestFriendList(att)) {

            byte[] buffer = new byte[BUFFER_SIZE];

            Log("Start patching file: " + filename);
            String temporatyFile = filename + SUBFIX_VEP_TOOL_TEMPLATE_ZIP_FILE;
            FileOutputStream fos = new FileOutputStream(temporatyFile);
            JarOutputStream jaros = new JarOutputStream(fos, jarfile.getManifest());

            Enumeration<JarEntry> jarEntries = jarfile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();

                // Copy bug ignore manifest entry
                if (!jarEntry.getName().equalsIgnoreCase(META_INF_MANIFESTM_ENTRY)) {

                    InputStream entryIs = jarfile.getInputStream(jarEntry);
                    jaros.putNextEntry(new JarEntry(jarEntry.getName()));

                    int bytesRead = 0;
                    while ((bytesRead = entryIs.read(buffer)) != -1) {
                        jaros.write(buffer, 0, bytesRead);
                    }
                    entryIs.close();
                    jaros.flush();
                    jaros.closeEntry();
                }
            }

            jaros.close();
            fos.close();
            jarfile.close();

            Log("Replace old file");
            replaceFile(filename, temporatyFile);
        } else {
            Log("No need to patch file: " + filename);
        }
    }

    /**
     * Put Viettel Enterprise Platform Tools to friend list
     *
     * @param oldFriendList old friend list string
     * @return new friend list string
     */
    private static String putViettelTooltoFriendList(String oldFriendList) {
        if (oldFriendList == null || oldFriendList.isEmpty()) {
            return VEP_TOOLS_MODULE_NAME;
        }

        if (oldFriendList.contains(VEP_TOOLS_MODULE_NAME)) {
            return oldFriendList;
        } else {
            String newFriendList = oldFriendList.trim();
            if (newFriendList.endsWith(",")) {
                newFriendList += " " + VEP_TOOLS_MODULE_NAME;
            } else {
                newFriendList += ", " + VEP_TOOLS_MODULE_NAME;
            }
            return newFriendList;
        }
    }

    /**
     * Update manifest attributes
     *
     * @param att Manifest Main Attributes
     * @return true if the Attributes need to modified (already modified)
     */
    private static boolean updateManifestFriendList(Attributes att) {
        for (Object key : att.keySet()) {

            if (key instanceof Attributes.Name) {
                Attributes.Name moduleKey = (Attributes.Name) key;

                // Looking for friends key
                if (moduleKey.toString().equalsIgnoreCase(NETBEANS_MODULE_FRIEND_KEY)) {
                    Object value = att.get(key);
                    if (value instanceof String) {

                        // Get old friend list
                        String oldFriendList = (String) value;

                        // Make new friend list
                        String newFriendList = putViettelTooltoFriendList(oldFriendList);

                        // Put new list in-place
                        att.put(key, newFriendList);

                        return true;
                    }
                }
            }
        }

        return false;
    }
}
