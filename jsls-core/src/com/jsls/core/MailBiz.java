package com.jsls.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.util.CollectionUtils;

import lombok.Data;

@Data
public class MailBiz {
    private String mailTo;
    private String mailCc;
    private List<String> attachmentList = new ArrayList<>();

    public static MailBiz of(String mailTo) {
        return of(mailTo, null);
    }

    public static MailBiz of(String mailTo, String mailCc) {
        MailBiz mailBiz = new MailBiz();
        mailBiz.setMailTo(mailTo);
        mailBiz.setMailCc(mailCc);
        return mailBiz;
    }

    public void apply(ExportBiz exportBiz) {
        apply(exportBiz, exportBiz::useSubPath);
    }

    public void apply(ExportBiz exportBiz, Function<String, String> attachFn) {
        Set<String> fileList = exportBiz.getFileList();
        if (!CollectionUtils.isEmpty(fileList)) {
            for (String fileName : fileList) {
                attachmentList.add(attachFn.apply(fileName));
            }
        }
    }
}
