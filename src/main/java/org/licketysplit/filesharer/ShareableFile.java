package org.licketysplit.filesharer;

import org.licketysplit.securesocket.*;

import java.io.File;

public class ShareableFile extends File {
    private File file;

    public ShareableFile(String pathname){
        super(pathname);
    }
}
