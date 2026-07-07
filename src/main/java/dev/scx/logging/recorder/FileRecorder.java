package dev.scx.logging.recorder;

import dev.scx.logging.ScxLogRecord;
import dev.scx.logging.ScxLogRecorder;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static dev.scx.logging.recorder.ScxLogRecordHelper.formatLogRecord;
import static java.nio.file.StandardOpenOption.*;

/// FileRecorder (非 final, 允许子类继承并重写 format)
///
/// @author scx567888
public class FileRecorder implements ScxLogRecorder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path storedDirectory;

    public FileRecorder(Path storedDirectory) {
        this.storedDirectory = storedDirectory;
    }

    public static void writeToFile(Path path, String data) {
        // 同步写入, 同时忽略所有异常.
        try {
            Files.writeString(path, data, APPEND, CREATE, SYNC, WRITE);
        } catch (NoSuchFileException e) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, data, APPEND, CREATE, SYNC, WRITE);
            } catch (Exception _) {

            }
        } catch (Exception _) {

        }
    }

    public String getLogFileName(TemporalAccessor temporal) {
        return DATE_TIME_FORMATTER.format(temporal) + ".log";
    }

    @Override
    public void record(ScxLogRecord logRecord) {
        var directory = storedDirectory;
        if (directory == null) {
            return;
        }
        var data = format(logRecord);
        var logFileName = getLogFileName(logRecord.timeStamp());
        var path = directory.resolve(logFileName);
        writeToFile(path, data);
    }

    public String format(ScxLogRecord logRecord) {
        return formatLogRecord(logRecord);
    }

}
