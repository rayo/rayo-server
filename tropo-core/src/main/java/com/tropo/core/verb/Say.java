package com.tropo.core.verb;

public class Say extends BaseVerb {

    private String voice;
    private PromptItems promptItems;

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public PromptItems getPromptItems() {
        return promptItems;
    }

    public void setPromptItems(PromptItems items) {
        this.promptItems = items;
    }

}
