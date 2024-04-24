package com.jsls.core;

import java.util.Date;

import com.jsls.util.DateUtils;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import com.xxl.job.core.context.XxlJobHelper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobScene<D> extends Progress.Recorder {
    private Date processTime;
    private D data;

    public JobScene(String jobName) {
        this(jobName, 100);
    }

    public JobScene(String jobName, int pageSize) {
        super(jobName, pageSize);
        this.processTime = new Date();
    }

    @Override
    public JobScene<D> copy() {
        JobScene<D> me = this;
        JobScene<D> use = new JobScene<D>(getTaskName(), getBatchSize()) {
            @Override
            public void log(String format, Object... arguments) {
                me.log(format, arguments);
            }

            @Override
            public void log(Throwable e) {
                me.log(e);
            }
        };
        use.copyFrom(this);
        return use;
    }

    public void copyFrom(JobScene<D> source) {
        this.data = source.data;
        this.processTime = source.processTime;
    }

    /**
     * mock处理时间
     * 
     * @param time
     */
    public void applyMockProcessTime(String time) {
        if (StringUtils.hasText(time)) {
            Date mockDate = DateUtils.smartParseDate(time);
            if (mockDate != null) {
                this.setProcessTime(mockDate);
            }
        }
    }

    public static String useJobParam(String param) {
        if (XxlJobHelper.getJobId() != -1) {
            return XxlJobHelper.getJobParam();
        }
        return param;
    }

    public static <B> JobScene<B> useJobScene(String jobName, int pageSize) {
        if (XxlJobHelper.getJobId() != -1) {
            return useXxlJobScene(jobName, pageSize);
        }
        return new JobScene<B>(jobName, pageSize);
    }

    public static <B> JobScene<B> useJobScene(String jobName, int pageSize, Logger logger) {
        if (XxlJobHelper.getJobId() != -1) {
            return useXxlJobScene(jobName, pageSize);
        }
        return new JobScene<B>(jobName, pageSize) {
            @Override
            public void log(String format, Object... arguments) {
                logger.info(format, arguments);
            }

            @Override
            public void log(Throwable e) {
                logger.error(useTaskName() + "异常:" + e.getMessage(), e);
            }
        };
    }

    private static <B> JobScene<B> useXxlJobScene(String jobName, int pageSize) {
        return new JobScene<B>(jobName, pageSize) {
            @Override
            public void log(String format, Object... arguments) {
                XxlJobHelper.log(format, arguments);
            }

            @Override
            public void log(Throwable e) {
                XxlJobHelper.log(e);
            }
        };
    }
}
