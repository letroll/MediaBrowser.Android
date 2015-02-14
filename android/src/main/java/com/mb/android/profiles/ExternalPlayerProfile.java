package com.mb.android.profiles;

import mediabrowser.model.dlna.CodecProfile;
import mediabrowser.model.dlna.CodecType;
import mediabrowser.model.dlna.DlnaProfileType;
import mediabrowser.model.dlna.DirectPlayProfile;
import mediabrowser.model.dlna.ProfileCondition;
import mediabrowser.model.dlna.ProfileConditionType;
import mediabrowser.model.dlna.ProfileConditionValue;
import mediabrowser.model.dlna.profiles.DefaultProfile;

/**
 * Created by Mark on 2014-11-13.
 */
public class ExternalPlayerProfile extends DefaultProfile {

    public ExternalPlayerProfile() {
        buildDirectPlayProfiles();
    }

    private void buildDirectPlayProfiles() {

        DirectPlayProfile aviProfile = new DirectPlayProfile();
        aviProfile.setContainer("avi");
        aviProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile mp4Profile = new DirectPlayProfile();
        mp4Profile.setContainer("mp4,mov");
        mp4Profile.setType(DlnaProfileType.Video);

        DirectPlayProfile asfProfile = new DirectPlayProfile();
        asfProfile.setContainer("asf");
        asfProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile mkvProfile = new DirectPlayProfile();
        mkvProfile.setContainer("mkv");
        mkvProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile threegpProfile = new DirectPlayProfile();
        threegpProfile.setContainer("3gp");
        threegpProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile mp3Profile = new DirectPlayProfile();
        mp3Profile.setContainer("mp3");
        mp3Profile.setType(DlnaProfileType.Audio);

        DirectPlayProfile vorbisProfile = new DirectPlayProfile();
        vorbisProfile.setContainer("ogg");
        vorbisProfile.setType(DlnaProfileType.Audio);

        DirectPlayProfile flacProfile = new DirectPlayProfile();
        flacProfile.setContainer("flac");
        flacProfile.setType(DlnaProfileType.Audio);

        DirectPlayProfile jpegProfile = new DirectPlayProfile();
        jpegProfile.setContainer("jpg,jpeg");
        jpegProfile.setType(DlnaProfileType.Photo);

        DirectPlayProfile gifProfile = new DirectPlayProfile();
        gifProfile.setContainer("gif");
        gifProfile.setType(DlnaProfileType.Photo);

        DirectPlayProfile pngProfile = new DirectPlayProfile();
        pngProfile.setContainer("png");
        pngProfile.setType(DlnaProfileType.Photo);

        DirectPlayProfile webpProfile = new DirectPlayProfile();
        webpProfile.setContainer("webp");
        webpProfile.setType(DlnaProfileType.Photo);

        setDirectPlayProfiles(new mediabrowser.model.dlna.DirectPlayProfile[] {
                aviProfile,
                mp4Profile,
                asfProfile,
                mkvProfile,
                threegpProfile,
                mp3Profile,
                vorbisProfile,
                flacProfile,
                jpegProfile,
                gifProfile,
                pngProfile,
                webpProfile
        });


    }
}
