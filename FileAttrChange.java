package sdtest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Set;

/**
 * When copying image files from a memory card to PC, the file modification date can be adjusted by Windows which causes some
 * programs to consider the photo to have been taken on a different date.
 * <p>
 * This utility program takes a given directory and updates image/video files to set each file's modification date to that file's
 * creation date.
 * <p>
 * The source folder name is currently hard-coded for my convenience, but could easily be a command line parameter if desired.
 */
public class FileAttrChange
{
    private static final Set<String> VAL_SUFFIXES;

    private static final String SOURCE_FOLDER_NAME = "H:\\Photos\\2019\\2019_05_03-Holiday";

    static
    {
        VAL_SUFFIXES = new HashSet<String>();

        VAL_SUFFIXES.add("jpg");
        VAL_SUFFIXES.add("mp4");
        VAL_SUFFIXES.add("avi");
    }

    public static void main(String[] args)
    {
        String sourceFolderName = SOURCE_FOLDER_NAME;

        try
        {
            adjustFiles(sourceFolderName);

            System.out.println("Successfully adjusted all images/videos");
        }
        catch (ValException ve)
        {
            System.out.println(ve.getMessage());
        }
    }

    private static void adjustFiles(String inSourceFolderName) throws ValException
    {
        File sourceFolder = new File(inSourceFolderName);

        // Check whether source exists and it is folder.
        if (!sourceFolder.exists() || !sourceFolder.isDirectory())
            throw new ValException("Specified [" + inSourceFolderName + "] does not exist or is not a valid directory");

        // Get list of the files and iterate over them
        File[] listOfFiles = sourceFolder.listFiles();

        if (listOfFiles == null)
            throw new ValException("Cannot obtain list of files from [" + inSourceFolderName + ']');

        for (File child : listOfFiles)
        {
            if (child.isDirectory())
                continue;

            String childName = child.getName();
            String childNameLowercase = childName.toLowerCase();
            int dotIndex = childNameLowercase.lastIndexOf('.');

            if (dotIndex == -1)
            {
                System.out.println("NOTE: skipping file: " + childName);

                continue;
            }

            String suffix = childNameLowercase.substring(dotIndex + 1);

            if (!VAL_SUFFIXES.contains(suffix))
            {
                System.out.println("NOTE: skipping file: " + childName);

                continue;
            }

            // Get attributes.
            Path childPath = Paths.get(child.getAbsolutePath());
            BasicFileAttributes attr;
            try
            {
                attr = Files.readAttributes(childPath, BasicFileAttributes.class);
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();

                throw new ValException("Cannot read attributes for file [" + child.getAbsolutePath() + ']');
            }

//DEBUG
//                System.out.println("creationTime: " + attr.creationTime());
//                System.out.println("lastModTime: " + attr.lastModifiedTime());

            FileTime fileTime = attr.creationTime();

            child.setLastModified(fileTime.toMillis());
        }
    }

    public static class ValException extends Exception
    {
        public ValException(String inMessage)
        {
            super(inMessage);
        }
    }
}
