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
        aviProfile.setVideoCodec("mpeg4");
        aviProfile.setAudioCodec("mp3");
        aviProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile aviProfile2 = new DirectPlayProfile();
        aviProfile2.setContainer("avi");
        aviProfile2.setVideoCodec("h264");
        aviProfile2.setAudioCodec("aac");
        aviProfile2.setType(DlnaProfileType.Video);

        DirectPlayProfile mp4Profile = new DirectPlayProfile();
        mp4Profile.setContainer("mp4,mov");
        mp4Profile.setVideoCodec("h264,mpeg4");
        mp4Profile.setAudioCodec("aac,ac3");
        mp4Profile.setType(DlnaProfileType.Video);

        DirectPlayProfile asfProfile = new DirectPlayProfile();
        asfProfile.setContainer("asf");
        asfProfile.setVideoCodec("wmv2,wmv3,vc1");
        asfProfile.setAudioCodec("wmav2,wmapro");
        asfProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile mkvProfile = new DirectPlayProfile();
        mkvProfile.setContainer("mkv");
        mkvProfile.setVideoCodec("vpx");
        mkvProfile.setAudioCodec("vorbis");
        mkvProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile threegpProfile = new DirectPlayProfile();
        threegpProfile.setContainer("3gp");
        threegpProfile.setVideoCodec("mpeg4,h264");
        threegpProfile.setAudioCodec("aac,amr");
        threegpProfile.setType(DlnaProfileType.Video);

        DirectPlayProfile mp3Profile = new DirectPlayProfile();
        mp3Profile.setContainer("mp3");
        mp3Profile.setAudioCodec("mp3");
        mp3Profile.setType(DlnaProfileType.Audio);

        DirectPlayProfile vorbisProfile = new DirectPlayProfile();
        vorbisProfile.setContainer("ogg");
        vorbisProfile.setAudioCodec("vorbis");
        vorbisProfile.setType(DlnaProfileType.Audio);

        DirectPlayProfile flacProfile = new DirectPlayProfile();
        flacProfile.setContainer("flac");
        flacProfile.setAudioCodec("flac");
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
                aviProfile2,
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
