/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.server.storage.filesystem;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.protobuf.Messages;
import org.spine3.util.FileNameEscaper;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.propagate;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static java.nio.file.Files.copy;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.spine3.util.Identifiers.idToString;

//TODO:2015-10-27:alexander.yevsyukov: Refactor to move methods to corresponding classes.
/**
 * Utility class for working with file system.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
class FileSystemHelper {

    protected static final String COMMAND_STORE_FILE_NAME = "/command-store";
    protected static final String EVENT_STORE_FILE_NAME = "/event-store";
    private static final String AGGREGATE_FILE_NAME_PREFIX = "/aggregate/";
    protected static final String PATH_DELIMITER = "/";
    private static final String ENTITY_STORE_DIR = "/entity-store/";

    //TODO:2015-10-27:alexander.yevsyukov: What would happen if we want to use this class with two executors?
    @SuppressWarnings("StaticNonFinalField")
    private static String fileStorePath = null;

    @SuppressWarnings("StaticNonFinalField")
    private static String commandStoreFilePath = null;

    @SuppressWarnings("StaticNonFinalField")
    private static String eventStoreFilePath = null;

    protected static final String NOT_CONFIGURED_MESSAGE = "Helper is not configured. Call 'configure' method.";

    @SuppressWarnings("StaticNonFinalField")
    private static File backup = null;

    private FileSystemHelper() {}

    /**
     * Configures helper with file storage path.
     *
     * @param executorClass execution context class. Is used to choose target directory for storage.
     */
    protected static void configure(Class executorClass) {

        final String tempDir = getTempDir().getAbsolutePath();

        fileStorePath = tempDir + PATH_DELIMITER + executorClass.getSimpleName();
        commandStoreFilePath = fileStorePath + COMMAND_STORE_FILE_NAME;
        eventStoreFilePath = fileStorePath + EVENT_STORE_FILE_NAME;
    }

    /**
     * Writes {@link Message} into {@link File} using {@link Message#writeDelimitedTo}.
     *
     * @param file    the {@link File} to write data in
     * @param message the data to extract
     */
    @SuppressWarnings({"TypeMayBeWeakened", "ResultOfMethodCallIgnored", "OverlyBroadCatchBlock"})
    protected static void writeMessage(File file, Message message) {

        FileOutputStream fileOutputStream = null;
        OutputStream bufferedOutputStream = null;

        try {
            if (file.exists()) {
                backup = makeBackupCopy(file);
            } else {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            fileOutputStream = new FileOutputStream(file, true);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            message.writeDelimitedTo(bufferedOutputStream);

            if (backup != null) {
                backup.delete();
            }
        } catch (IOException ignored) {
            restoreFromBackup(file);
        } finally {
            flushAndCloseSilently(fileOutputStream, bufferedOutputStream);
        }
    }

    /**
     * Reads {@link Message} from {@link File}.
     *
     * @param file the {@link File} to read from.
     */
    protected static Message readMessage(File file) {

        checkFileExists(file);

        InputStream fileInputStream = open(file);
        InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        Any any;

        try {
            any = Any.parseDelimitedFrom(bufferedInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read message from file: " + file.getAbsolutePath(), e);
        } finally {
            closeSilently(fileInputStream, bufferedInputStream);
        }

        final Message result = Messages.fromAny(any);
        return result;
    }

    /**
     * Removes all data from the file system storage.
     */
    public static void deleteAll() {

        final File folder = new File(getFileStorePath());
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }

        try {
            deleteDirectory(folder);
        } catch (IOException e) {
            propagate(e);
        }
    }

    protected static String getFileStorePath() {
        return fileStorePath;
    }

    /**
     * Returns path for aggregate storage file.
     *
     * @param aggregateType     the type of an aggregate
     * @param aggregateIdString String representation of aggregate id
     * @return absolute path string
     */
    protected static String getAggregateFilePath(String aggregateType, String aggregateIdString) {
        checkConfigured();

        final String filePath = fileStorePath + AGGREGATE_FILE_NAME_PREFIX +
                aggregateType + PATH_DELIMITER + aggregateIdString;
        return filePath;
    }

    /**
     * Returns common command store file path.
     *
     * @return absolute path string
     */
    protected static String getCommandStoreFilePath() {
        checkConfigured();
        return commandStoreFilePath;
    }

    /**
     * Returns common event store file path.
     *
     * @return absolute path string
     */
    protected static String getEventStoreFilePath() {
        checkConfigured();
        return eventStoreFilePath;
    }

    protected static String getEntityStoreFilePath(String entityId) {
        checkConfigured();
        final String filePath = fileStorePath + ENTITY_STORE_DIR + entityId;
        return filePath;
    }

    protected static String getBackupFilePath(File sourceFile) {
        checkConfigured();
        final String path = sourceFile.toPath() + "_backup";
        return path;
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    protected static void checkConfigured() {
        if (isNullOrEmpty(fileStorePath)) {
            throw new IllegalStateException(NOT_CONFIGURED_MESSAGE);
        }
    }

    /**
     * Closes passed closeables one by one silently.
     * <p/>
     * Logs each {@link IOException} if it occurs.
     */
     @SuppressWarnings("ConstantConditions")
    public static void closeSilently(@Nullable Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        try {
            for (Closeable c : closeables) {
                if (c != null) {
                    c.close();
                }
            }
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Exception while closing stream", e);
            }
        }
    }

    /**
     * Flushes passed streams one by one.
     * <p/>
     * Logs each {@link IOException} if it occurs.
     */
    public static void flushSilently(@Nullable Flushable... flushables) {
        try {
            flush(flushables);
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Exception while flushing stream", e);
            }
        }
    }

    /**
     * Flushes streams in turn.
     *
     * @throws java.lang.RuntimeException if {@link IOException} occurs
     */
    public static void tryToFlush(@Nullable Flushable... flushables) {
        try {
            flush(flushables);
        } catch (IOException e) {
            propagate(e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void flush(@Nullable Flushable[] flushables) throws IOException {
        if (flushables == null) {
            return;
        }
        for (Flushable f : flushables) {
            if (f != null) {
                f.flush();
            }
        }
    }

    /**
     * Flushes and closes output streams in turn silently. Logs IOException if occurs.
     */
    public static void flushAndCloseSilently(@Nullable OutputStream... streams) {
        if (streams == null) {
            return;
        }
        flushSilently(streams);
        closeSilently(streams);
    }

    /**
     * @param file file to check
     * @throws IllegalStateException if there is no such file
     */
    public static void checkFileExists(File file) {
        if (!file.exists()) {
            throw new IllegalStateException("No such file: " + file.getAbsolutePath());
        }
    }

    /**
     * Tries to open {@code FileInputStream} from file
     *
     * @throws RuntimeException if there is no such file
     */
    public static FileInputStream open(File file) {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            propagate(e);
        }

        return fileInputStream;
    }

    /**
     * Creates string representation of the passed ID escaping characters
     * that are not allowed in file names.
     *
     * @param id the ID to convert
     * @return string representation of the ID
     * @see org.spine3.util.Identifiers#idToString(Object)
     * @see org.spine3.util.FileNameEscaper#escape(String)
     */
    public static <I> String idToStringWithEscaping(I id) {

        final I idNormalized = escapeStringFieldsIfIsMessage(id);
        String result = idToString(idNormalized);
        result = FileNameEscaper.getInstance().escape(result);

        return result;
    }

    private static <I> I escapeStringFieldsIfIsMessage(I input) {
        I result = input;
        if (input instanceof Message) {
            final Message message = escapeStringFields((Message) input);
            @SuppressWarnings("unchecked")
            I castedMessage = (I) message; // cast is safe because input is Message
            result = castedMessage;
        }
        return result;
    }

    private static Message escapeStringFields(Message message) {

        final Message.Builder result = message.toBuilder();
        final Map<Descriptors.FieldDescriptor, Object> fields = message.getAllFields();

        final FileNameEscaper escaper = FileNameEscaper.getInstance();

        for (Descriptors.FieldDescriptor descriptor : fields.keySet()) {

            Object value = fields.get(descriptor);

            if (descriptor.getJavaType() == STRING) {
                value = escaper.escape(value.toString());
            }

            result.setField(descriptor, value);
        }

        return result.build();
    }

    private static void restoreFromBackup(File file) {
        boolean isDeleted = file.delete();
        if (isDeleted && backup != null) {
            //noinspection ResultOfMethodCallIgnored
            backup.renameTo(file);
        }
    }

    private static File makeBackupCopy(File sourceFile) throws IOException {
        final String backupFilePath = getBackupFilePath(sourceFile);
        File backupFile = new File(backupFilePath);

        copy(sourceFile.toPath(), backupFile.toPath());

        return backupFile;
    }

    private static File getTempDir() {
        try {
            File tmpFile = File.createTempFile("temp-dir-check", ".tmp");
            File result = new File(tmpFile.getParent());
            if (tmpFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmpFile.delete();
            }
            return result;
        } catch (IOException e) {
            propagate(e);
        }
        throw new IllegalStateException("Unable to get temporary directory for storage");
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FileSystemHelper.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
