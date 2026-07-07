package dev.scx.logging.recorder;

import dev.scx.logging.ScxLogRecord;
import dev.scx.logging.ScxLogRecorder;

import static dev.scx.logging.recorder.ScxLogRecordHelper.formatLogRecord;
import static java.lang.System.Logger.Level.ERROR;

/// ConsoleRecorder (非 final, 允许子类继承并重写 format)
///
/// @author scx567888
public class ConsoleRecorder implements ScxLogRecorder {

    @Override
    public void record(ScxLogRecord logRecord) {
        var data = format(logRecord);
        // 错误级别的我们就采用 err 打印, 其余的 正常输出
        if (logRecord.level().getSeverity() >= ERROR.getSeverity()) {
            System.err.print(data);
        } else {
            System.out.print(data);
        }
    }

    public String format(ScxLogRecord logRecord) {
        return formatLogRecord(logRecord);
    }

}
