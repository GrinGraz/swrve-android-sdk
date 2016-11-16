package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.rest.SwrveFilterInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.swrve.sdk.SwrveHelper.LOG_TAG;

class SwrveAssetsManagerImp implements SwrveAssetsManager {

    protected Set<String> assetsOnDisk = new HashSet<>();

    protected final Context context;
    protected String cdnImages;
    protected String cdnFonts;
    protected File storageDir;

    protected SwrveAssetsManagerImp(Context context) {
        this.context = context;
    }

    @Override
    public void setCdnImages(String cdnImages) {
        this.cdnImages = cdnImages;
    }

    @Override
    public void setCdnFonts(String cdnFonts) {
        this.cdnFonts = cdnFonts;
    }

    @Override
    public void setStorageDir(File storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public File getStorageDir() {
        return storageDir;
    }

    @Override
    public Set<String> getAssetsOnDisk() {
        synchronized (assetsOnDisk) {
            return this.assetsOnDisk;
        }
    }

    @Override
    public void downloadAssets(Set<SwrveAssetsQueueItem> assetsQueueImages, Set<SwrveAssetsQueueItem> assetsQueueFonts, SwrveAssetsCompleteCallback callback) {

        if (!storageDir.canWrite()) {
            SwrveLogger.e(LOG_TAG, "Could not download assets because do not have write access to storageDir:" + storageDir);
        } else {
            downloadAssets(assetsQueueImages, cdnImages);
            downloadAssets(assetsQueueFonts, cdnFonts);
        }
        if (callback != null) {
            callback.complete();
        }
    }

    protected void downloadAssets(final Set<SwrveAssetsQueueItem> assetsQueue, final String cdnRoot) {
        if (SwrveHelper.isNullOrEmpty(cdnRoot)) {
            SwrveLogger.e(LOG_TAG, "Error downloading assets. No cdnRoot url.");
            return;
        }
        if(assetsQueue == null) {
            return;
        }

        Set<SwrveAssetsQueueItem> assetsToDownload = filterExistingFiles(assetsQueue);
        for (SwrveAssetsQueueItem assetItem : assetsToDownload) {
            boolean success = downloadAsset(assetItem, cdnRoot);
            if (success) {
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(assetItem.getName()); // store the font name
                }
            }
        }
    }

    protected Set<SwrveAssetsQueueItem> filterExistingFiles(Set<SwrveAssetsQueueItem> assetsQueue) {
        Iterator<SwrveAssetsQueueItem> itDownloadQueue = assetsQueue.iterator();
        while (itDownloadQueue.hasNext()) {
            SwrveAssetsQueueItem item = itDownloadQueue.next();
            File file = new File(storageDir, item.getName());
            if (file.exists()) {
                itDownloadQueue.remove();
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(item.getName()); // store the font name
                }
            }
        }
        return assetsQueue;
    }

    protected boolean downloadAsset(final SwrveAssetsQueueItem assetItem, final String cdnRoot) {
        boolean success = false;
        String url = cdnRoot + assetItem.getName();
        InputStream inputStream = null;
        try {
            URLConnection openConnection = new URL(url).openConnection();
            inputStream = new SwrveFilterInputStream(openConnection.getInputStream());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stream.write(buffer, 0, bytesRead);
            }
            byte[] fileContents = stream.toByteArray();
            String sha1File = SwrveHelper.sha1(stream.toByteArray());
            if (assetItem.getDigest().equals(sha1File)) {
                FileOutputStream fileStream = new FileOutputStream(new File(storageDir, assetItem.getName()));
                fileStream.write(fileContents); // Save to file
                fileStream.close();
                success = true;
            } else {
                SwrveLogger.e(LOG_TAG, "Error downloading assetItem:" + assetItem + ". Did not match digest:" + sha1File);
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error downloading asset:" + assetItem, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    SwrveLogger.e(LOG_TAG, "Error closing assets stream.", e);
                }
            }
        }
        return success;
    }
}