package de.gerdiproject.store.impl;

import de.gerdiproject.store.datamodel.TaskElement;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ResearchDataInputStream extends InputStream {

    private long copiedSize = 0;
    private final long size;
    private final InputStream in;
    private final TaskElement progressEntry;

    public ResearchDataInputStream(URL url, TaskElement progressEntry) throws IOException {
        this.in = url.openStream();
        this.size = url.openConnection().getContentLengthLong();
        this.progressEntry = progressEntry;
        if (this.size == -1) this.progressEntry.setProgressInPercent(new Integer(-1));
    }

    @Override
    public int read() throws IOException {
        this.copiedSize++;
        if(this.copiedSize % 1000 == 0) this.updateEntry();
        return in.read();
    }

    private void updateEntry(){
        if (size == -1) return;
        System.out.print(copiedSize + " / " + size + " = ");
        System.out.println((int) (copiedSize * 100 / size));
        this.progressEntry.setProgressInPercent((int) (copiedSize * 100 / size));
    }

}
