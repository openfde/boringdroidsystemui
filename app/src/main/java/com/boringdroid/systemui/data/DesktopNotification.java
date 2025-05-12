package com.boringdroid.systemui.data;

import android.app.Notification;
import android.os.Parcel;
import android.os.Parcelable;
import android.app.PendingIntent;

public class DesktopNotification implements Parcelable {
    public int id;
    public String name;
    public String title;
    public String content;
    public String mediaStyle;
    public String packageName;
    public String computeElapsedTime;
    public String notificationText;
    public PendingIntent contentIntent;
    public boolean isClearable;
    public long postTime;
    public Notification.Action[] actions;

    public DesktopNotification() {
    }

    protected DesktopNotification(Parcel in) {
        id = in.readInt();
        name = in.readString();
        title = in.readString();
        content = in.readString();
        packageName = in.readString();
        computeElapsedTime = in.readString();
        notificationText = in.readString();
        contentIntent = in.readParcelable(PendingIntent.class.getClassLoader());
        isClearable = in.readBoolean();
        postTime = in.readLong();
        actions = in.createTypedArray(Notification.Action.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(packageName);
        dest.writeString(computeElapsedTime);
        dest.writeString(notificationText);
        dest.writeParcelable(contentIntent, flags);
        dest.writeBoolean(isClearable);
        dest.writeLong(postTime);
        dest.writeTypedArray(actions, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DesktopNotification> CREATOR = new Creator<DesktopNotification>() {
        @Override
        public DesktopNotification createFromParcel(Parcel in) {
            return new DesktopNotification(in);
        }

        @Override
        public DesktopNotification[] newArray(int size) {
            return new DesktopNotification[size];
        }
    };

    @Override
    public String toString() {
        return "DesktopNotification{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", packageName='" + packageName + '\'' +
                ", computeElapsedTime='" + computeElapsedTime + '\'' +
                ", notificationText='" + notificationText + '\'' +
                ", contentIntent=" + contentIntent +
                ", isClearable=" + isClearable +
                ", postTime=" + postTime +
                '}';
    }
}
