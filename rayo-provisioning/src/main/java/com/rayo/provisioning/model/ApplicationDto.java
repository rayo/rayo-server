package com.rayo.provisioning.model;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

public class ApplicationDto extends BaseDto {
    private String id;
    private String name;
    private URI platform;
    private String voicePpid;
    private URI voiceUrl;
    private String messagingPpid;
    private URI messagingUrl;
    private URI partition;
    private Boolean eventNotificationEnabled;
    private Boolean resultNotificationEnabled;
    
    @Override
    public String toString() {
        return String.format("[ApplicationDto %s:%s:%s:%s]", id, partition, platform, name);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(Integer id) {
        if (id != null) {
            this.id = id.toString();
        } else {
            this.id = null;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        try {
			this.platform = getUri(URLEncoder.encode(platform, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    }

    public void setPlatform(URI platform) {
        this.platform = platform;
    }

    public URI getVoiceUrl() {
        return voiceUrl;
    }

    public void setVoiceUrl(String voiceUrl) {
        this.voiceUrl = getUri(voiceUrl);
    }

    public void setVoiceUrl(URI voiceUrl) {
        this.voiceUrl = voiceUrl;
    }

    public URI getMessagingUrl() {
        return messagingUrl;
    }

    public void setMessagingUrl(String messagingUrl) {
        this.messagingUrl = getUri(messagingUrl);
    }

    public void setMessagingUrl(URI messagingUrl) {
        this.messagingUrl = messagingUrl;
    }

    public URI getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = getUri(partition);
    }

    public void setPartition(URI partition) {
        this.partition = partition;
    }

    public Boolean getEventNotificationEnabled() {
        return eventNotificationEnabled;
    }

    public void setEventNotificationEnabled(Boolean eventNotificationEnabled) {
        this.eventNotificationEnabled = eventNotificationEnabled;
    }

    public Boolean getResultNotificationEnabled() {
        return resultNotificationEnabled;
    }

    public void setResultNotificationEnabled(Boolean resultNotificationEnabled) {
        this.resultNotificationEnabled = resultNotificationEnabled;
    }

	public String getVoicePpid() {
		return voicePpid;
	}

	public void setVoicePpid(String voicePpid) {
		this.voicePpid = voicePpid;
	}

	public String getMessagingPpid() {
		return messagingPpid;
	}

	public void setMessagingPpid(String messagingPpid) {
		this.messagingPpid = messagingPpid;
	}
}
