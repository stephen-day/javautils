package sdtest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * This utility program takes a directory of images/videos and sorts them into date-named sub-directories.
 * <p>
 * There are two modes supported:
 * <ol>
 *   <li>Filename based (where the filename contains a date).</li>
 *   <li>Creation-date based.</li>
 * </ol>
 * <p>
 * The mode and source folder name are currently hard-coded for my convenience, but could easily be command line parameters if
 * desired.
 * <p>
 * I like to put a suffix on the folders (different suffixes for different phones/cameras). This values should also be specified
 * before running the program.
 */
public class PicMove
{
    private static final Set<String> VAL_SUFFIXES;
    private static final Pattern DATE_PATTERN = Pattern.compile("^[1-2][0-9][0-9][0-9][0-1][0-9][0-3][0-9]$");

    /**
     * When true, file name must start with "yyyymmdd_" (e.g. "20181225_xxxx.jpg").
     * <p>
     * When false, uses the file creation date.
     */
    private static final boolean IS_FILENAME_BASED = true;

    private static final String SOURCE_FOLDER_NAME = "D:\\Photo_upload";

    private static final String DEST_FOLDER_SUFFIX = "z-";

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
            moveFiles(sourceFolderName);

            System.out.println("Successfully moved all images/videos");
        }
        catch (ValException ve)
        {
            System.out.println(ve.getMessage());
        }

        // I normally run this program from Eclipse. This sleep allows me to see the above message rather than it disappearing.
        // There is no functional reason for this sleep. Remove it if adapting to run from command line.
        System.out.println("\nWaiting for 5 seconds...");

        try
        {
            Thread.sleep(5);
        }
        catch (InterruptedException ie)
        {
            System.out.println("Sleep interrupted...");
        }
    }

    private static void moveFiles(String inSourceFolderName) throws ValException
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
                System.out.println("SJD: skipping file: " + childName);

                continue;
            }

            String suffix = childNameLowercase.substring(dotIndex + 1);

            if (!VAL_SUFFIXES.contains(suffix))
            {
                System.out.println("SJD: skipping file: " + childName);

                continue;
            }

            // Calculate the destination directory name
            String destDirName;

            if (IS_FILENAME_BASED)
            {
                int underscIndex = childNameLowercase.indexOf('_');

                if (underscIndex == -1)
                {
                    System.out.println("SJD: skipping file: " + childName);

                    continue;
                }

                String dateStr = childNameLowercase.substring(0, underscIndex);

                if (dateStr.length() != 8 || !DATE_PATTERN.matcher(dateStr).matches())
                {
                    System.out.println("SJD: skipping file: " + childName + " (does not start with 'date_')");

                    continue;
                }

                destDirName = dateStr.substring(0, 4) + '_' +
                              dateStr.substring(4, 6) + '_' +
                              dateStr.substring(6);
            }
            else  // creation date based.
            {
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

// DEBUG
//              System.out.println("creationTime: " + attr.creationTime());
//              System.out.println("lastModTime: " + attr.lastModifiedTime());
//              String dateStr = attr.lastModifiedTime().toString();  // "YYYY-MM-DDThh:mm:..."
//              System.out.println("Default time zone: " + TimeZone.getDefault());

                // Convert GMT date/time to local time zone.
                // (ideally, we would know the time zone in which the photo was taken, but that doesn't seem available).
                Instant modTimeGmt = attr.lastModifiedTime().toInstant();
                Date modDateTime = Date.from(modTimeGmt);

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(modDateTime);

                //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");

                sdf.setTimeZone(TimeZone.getDefault());
                String dateStr = sdf.format(calendar.getTime());

                destDirName = dateStr;
            }

            File destFolder = new File(inSourceFolderName + '\\' + destDirName + DEST_FOLDER_SUFFIX);

            if (!destFolder.exists())
                destFolder.mkdirs();
            else if (!destFolder.isDirectory())
                throw new ValException("File exists with expected name for directory [" + destFolder + ']');

            // Move file to destination folder
            child.renameTo(new File(destFolder + "\\" + child.getName()));
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
