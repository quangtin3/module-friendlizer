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
import javax.swing.JTextArea;

/**
 * Make selected module friendly with Viettel Enterprise Platform Tools
 *
 * @author quanghx2@viettel.com.vn
 */
public class FriendlizerUtilities {

    static private final String NEWLINE = "\n";
    /**
     * List of modules to patch, for the Netbeans 7.2.1
     */
    private static String[] modules = new String[]{
        "ide/modules/org-netbeans-modules-html-editor-lib.jar",
        "java/modules/org-netbeans-modules-j2ee-persistence.jar",
        "java/modules/org-netbeans-modules-j2ee-persistenceapi.jar",
        "ide/modules/org-netbeans-modules-web-common.jar"};
    /**
     * Internal Manifest file key words
     */
    private static final String VEP_TOOLS_MODULE_NAME = "com.viettel.vep.tools";
    private static final String NETBEANS_MODULE_FRIEND_KEY = "OpenIDE-Module-Friends";
    private static final int BUFFER_SIZE = 4096 * 2;
    private static final String META_INF_MANIFESTM_ENTRY = "META-INF/MANIFEST.MF";
    private static final String SUBFIX_VEP_TOOL_TEMPLATE_ZIP_FILE = "_vep_tool_template.zip";

    public static boolean patchingNetBeans(String netbeansPath, JTextArea logger) {
        Log(logger, "Viettel Enterprise Platform - Module Friendlizer");

        File netbeansFolder = new File(netbeansPath);
        if (netbeansFolder.exists() && netbeansFolder.isDirectory()) {

            String ideFolder = netbeansPath.endsWith("/") ? netbeansPath : netbeansPath + "/";
            for (String module : modules) {
                Log(logger, "Examining " + module);

                String modulePath = ideFolder + module;
                if ((new File(modulePath)).isFile()) {
                    if (!patchingModuleJarFile(modulePath, logger)) {
                        return false;
                    }
                } else {
                    Log(logger, "Not a file");
                }
            }
        }
        return true;
    }

    /**
     * Log message to a logger.
     *
     * @param logger logger, in this example it is a JTextArea
     * @param message message to be logged
     */
    public static void Log(JTextArea logger, String message) {
        logger.append(message + NEWLINE);
        logger.setCaretPosition(logger.getDocument().getLength());
    }

    /**
     * Replace file
     *
     * @param targetPath target path
     * @param newPath new file path
     */
    private static boolean replaceFile(String targetPath, String newPath, JTextArea logger) {
        File targetFile = new File(targetPath);
        File newFile = new File(newPath);

        if (targetFile.exists()
                && targetFile.isFile()
                && newFile.exists()
                && newFile.canRead()) {

            targetFile.delete();
            try {
                Log(logger, "start replace");
                targetFile.createNewFile();
                return newFile.renameTo(targetFile);
            } catch (IOException e) {
                Log(logger, "replace error: " + e.getMessage());
                return false;
            }
        } else {
            Log(logger, "invalid input");
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
    private static boolean patchingModuleJarFile(String filename, JTextArea logger) {

        String temporatyFile = filename + SUBFIX_VEP_TOOL_TEMPLATE_ZIP_FILE;

        /* Start make new temporary file */
        JarFile jarfile = null;
        boolean ret;
        try {
            jarfile = new JarFile(filename);
            if (jarfile.getManifest() == null) {
                Log(logger, "Just ignore file without Manifest: " + filename);
                return true;
            }
            if (updateManifestFriendList(jarfile.getManifest().getMainAttributes())) {
                ret = copyAllJarEntries(jarfile, temporatyFile, logger);
            } else {
                Log(logger, "Nothing patched for: " + filename);
                return true;
            }

        } catch (IOException e) {
            ret = false;
            Log(logger, "Create temporaty file error: " + e.getMessage());
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
            Log(logger, "Replace old file");
            if (replaceFile(filename, temporatyFile, logger)) {
                Log(logger, "Replace successful!");
                return true;
            } else {
                Log(logger, "Can't replace file " + filename + " with file " + temporatyFile);
            }
        } else {
            Log(logger, "No temporary file created.");
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
    private static boolean copyAllJarEntries(JarFile jarfile, String newFile, JTextArea logger) {
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
            Log(logger, "Copy jarEntries to file " + newFile + " error: " + e.getMessage());
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
