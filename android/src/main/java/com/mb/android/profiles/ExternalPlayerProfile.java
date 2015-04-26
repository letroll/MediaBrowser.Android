package com.mb.android.profiles;

import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.DlnaProfileType;
import mediabrowser.model.dlna.DirectPlayProfile;
import mediabrowser.model.dlna.EncodingContext;
import mediabrowser.model.dlna.TranscodingProfile;

/**
 * Created by Mark on 2014-11-13.
 */
public class ExternalPlayerProfile extends DeviceProfile {

    public ExternalPlayerProfile() {
        buildDirectPlayProfiles();
        buildTranscodingProfiles();
    }

    private void buildTranscodingProfiles() {

        java.util.ArrayList<TranscodingProfile> transcodingProfiles = new java.util.ArrayList<TranscodingProfile>();

        TranscodingProfile tempVar = new TranscodingProfile();
        tempVar.setContainer("mp3");
        tempVar.setAudioCodec("mp3");
        tempVar.setType(DlnaProfileType.Audio);
        tempVar.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(tempVar);

        TranscodingProfile tempVar3 = new TranscodingProfile();
        tempVar3.setContainer("mp4");
        tempVar3.setVideoCodec("h264");
        tempVar3.setAudioCodec("aac");
        tempVar3.setType(DlnaProfileType.Video);
        tempVar3.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(tempVar3);

        TranscodingProfile tempVar4 = new TranscodingProfile();
        tempVar4.setContainer("jpg");
        tempVar4.setVideoCodec("jpg");
        tempVar4.setType(DlnaProfileType.Photo);
        tempVar4.setContext(EncodingContext.Streaming);
        transcodingProfiles.add(tempVar4);

        setTranscodingProfiles(transcodingProfiles.toArray(new TranscodingProfile[0]));
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
