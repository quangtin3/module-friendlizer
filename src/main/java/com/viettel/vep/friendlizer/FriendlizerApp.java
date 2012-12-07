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

/**
 * Make selected module friendly with Viettel Enterprise Platform Tools
 *
 * @author quanghx2@viettel.com.vn
 */
public class FriendlizerApp {

    private static final String IDE_ROOT_DIRECTORY_VAR = "/home/quanghx/modules/";
    /**
     * List of modules to patch
     */
    private static String[] modules = new String[]{
        "platform/modules/org-netbeans-modules-progress-ui.jar",
        "platform/modules/org-netbeans-modules-progress-ui.jar",};
    /**
     * Internal Manifest file key words
     */
    private static final String VEP_TOOLS_MODULE_NAME = "com.viettel.vep.tools";
    private static final String NETBEANS_MODULE_FRIEND_KEY = "OpenIDE-Module-Friends";
    private static final int BUFFER_SIZE = 4096 * 2;
    private static final String META_INF_MANIFESTM_ENTRY = "META-INF/MANIFEST.MF";
    private static final String SUBFIX_VEP_TOOL_TEMPLATE_ZIP_FILE = "_vep_tool_template.zip";

    public static void main(String[] args) throws IOException {
        Log("Viettel Enterprise Platform - Module Friendlizer");

        for (String module : modules) {
            Log("Examining " + module);
            String moduleFile = IDE_ROOT_DIRECTORY_VAR + module;

            if (module.endsWith("jar") && (new File(moduleFile)).isFile()) {
                patchingModuleJarFile(moduleFile);
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
    private static boolean replaceFile(String targetPath, String newPath) {
        File targetFile = new File(targetPath);
        File newFile = new File(newPath);

        if (targetFile.exists()
                && targetFile.isFile()
                && newFile.exists()
                && newFile.canRead()) {

            targetFile.delete();
            try {
                Log("start replace");
                targetFile.createNewFile();
                return newFile.renameTo(targetFile);
            } catch (IOException e) {
                Log("replace error: " + e.getMessage());
                return false;
            }
        } else {
            Log("invalid input");
            return false;
        }
    }

    /**
     * Patching module jar file procedure. For technique detail see:
     * http://javahowto.blogspot.com/2011/07/how-to-programmatically-copy-jar-files.html
     *
     * @param filename jar file to patch
     * @throws IOException error occur
     */
    private static boolean patchingModuleJarFile(String filename) {

        String temporatyFile = filename + SUBFIX_VEP_TOOL_TEMPLATE_ZIP_FILE;

        /* Start make new temporary file */
        JarFile jarfile = null;
        boolean ret;
        try {
            jarfile = new JarFile(filename);
            if (jarfile.getManifest() == null) {
                Log("Just ignore file without Manifest: " + filename);
                return true;
            }
            if (updateManifestFriendListIfNeed(jarfile.getManifest().getMainAttributes())) {
                ret = copyAllJarEntries(jarfile, temporatyFile);
            } else {
                Log("No need to patch this file: " + filename);
                return true;
            }

        } catch (IOException e) {
            ret = false;
            Log("Create temporaty file error: " + e.getMessage());
        } finally {
            if (jarfile != null) {
                try {
                    jarfile.close();
                } catch (IOException e) {
                }
            }
        }

        /* If we have new file, using it to replace the old one */
        if (ret) {
            Log("Replace old file");
            if (replaceFile(filename, temporatyFile)) {
                Log("Replace successful!");
                return true;
            } else {
                Log("Can't replace file " + filename + " with file " + temporatyFile);
            }
        } else {
            Log("No temporary file created.");
        }
        return false;
    }

    /**
     * Copy all jarFile entries to new file
     *
     * @param jarfile
     * @param newFile
     * @return
     * @throws IOException
     */
    private static boolean copyAllJarEntries(JarFile jarfile, String newFile) {
        byte[] buffer = new byte[BUFFER_SIZE];

        FileOutputStream fos = null;
        JarOutputStream jaros = null;
        try {
            fos = new FileOutputStream(newFile);
            jaros = new JarOutputStream(fos, jarfile.getManifest());

            Enumeration<JarEntry> jarEntries = jarfile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();

                // Copy bug ignore manifest entry
                if (!jarEntry.getName().equalsIgnoreCase(META_INF_MANIFESTM_ENTRY)) {

                    InputStream entryIs = jarfile.getInputStream(jarEntry);
                    jaros.putNextEntry(new JarEntry(jarEntry.getName()));

                    int bytesRead;
                    while ((bytesRead = entryIs.read(buffer)) != -1) {
                        jaros.write(buffer, 0, bytesRead);
                    }
                    entryIs.close();
                    jaros.flush();
                    jaros.closeEntry();
                }
            }

            jaros.flush();
            jaros.close();
            fos.close();
            return true;

        } catch (IOException e) {
            Log("Copy jarEntries to file " + newFile + " error: " + e.getMessage());
            return false;

        } finally {
            if (jaros != null) {
                try {

                    jaros.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {

                    fos.close();
                } catch (IOException e) {
                }
            }
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
    private static boolean updateManifestFriendListIfNeed(Attributes att) {
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
