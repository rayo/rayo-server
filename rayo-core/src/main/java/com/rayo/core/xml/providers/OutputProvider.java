package com.tropo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidationException;
import com.tropo.core.verb.Output;
import com.tropo.core.verb.OutputCompleteEvent;
import com.tropo.core.verb.OutputCompleteEvent.Reason;
import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.SeekCommand;
import com.tropo.core.verb.SpeedDownCommand;
import com.tropo.core.verb.SpeedUpCommand;
import com.tropo.core.verb.VolumeDownCommand;
import com.tropo.core.verb.VolumeUpCommand;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;

public class OutputProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:rayo:output:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:output:complete:1");

    private static final QName PAUSE_QNAME = new QName("pause", NAMESPACE);
    private static final QName RESUME_QNAME = new QName("resume", NAMESPACE);
    private static final QName SEEK_QNAME = new QName("seek", NAMESPACE);
    private static final QName SPEED_UP_QNAME = new QName("speed-up", NAMESPACE);
    private static final QName SPEED_DOWN_QNAME = new QName("speed-down", NAMESPACE);
    private static final QName VOLUME_UP_QNAME = new QName("volume-up", NAMESPACE);
    private static final QName VOLUME_DOWN_QNAME = new QName("volume-down", NAMESPACE);
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("output")) {
            return buildOutput(element);
        } else if (PAUSE_QNAME.equals(element.getQName())) {
            return buildPauseCommand(element);
        } else if (RESUME_QNAME.equals(element.getQName())) {
            return buildResumeCommand(element);
        } else if (SEEK_QNAME.equals(element.getQName())) {
            return buildSeekCommand(element);
        } else if (SPEED_UP_QNAME.equals(element.getQName())) {
            return buildSpeedUpCommand(element);
        } else if (SPEED_DOWN_QNAME.equals(element.getQName())) {
            return buildSpeedDownCommand(element);
        } else if (VOLUME_UP_QNAME.equals(element.getQName())) {
            return buildVolumeUpCommand(element);
        } else if (VOLUME_DOWN_QNAME.equals(element.getQName())) {
            return buildVolumeDownCommand(element);
        } else if (element.getNamespace().equals(COMPLETE_NAMESPACE)) {
            return buildCompleteCommand(element);
        }
        return null;
    }

    private Object buildCompleteCommand(Element element) {
        String reasonValue = element.getName().toUpperCase();
        Reason reason = Reason.valueOf(reasonValue);
        OutputCompleteEvent complete = new OutputCompleteEvent();
        complete.setReason(reason);
        return complete;
    }

    private Object buildOutput(Element element) throws URISyntaxException {
        
    	Output output = new Output();
        output.setPrompt(extractSsml(element));
        output.setVoice(element.attributeValue("voice"));

        if (element.attribute("interrupt-on") != null) {
        	output.setBargeinType(loadBargeinType(element));
        }
        if (element.attribute("start-offset") != null) {
            output.setStartOffset(toDuration("start-offset", element));
        }
        if (element.attribute("start-paused") != null) {
            output.setStartPaused(toBoolean("start-paused", element));
        }
        if (element.attribute("repeat-interval") != null) {
            output.setRepeatInterval(toDuration("repeat-interval", element));
        }
        if (element.attribute("repeat-times") != null) {
            output.setRepeatTimes(toInteger("repeat-times", element));
        }
        if (element.attribute("max-time") != null) {
            output.setMaxTime(toDuration("max-time", element));
        }

        return output;
    }

    private Object buildPauseCommand(Element element) {
        return new PauseCommand();
    }

    private Object buildResumeCommand(Element element) {
        return new ResumeCommand();
    }

    private Object buildSeekCommand(Element element) throws URISyntaxException {

    	SeekCommand command = new SeekCommand();
        if (element.attribute("direction") != null) {
        	command.setDirection(toEnum(SeekCommand.Direction.class, "direction", element));
        }
        if (element.attribute("amount") != null) {
        	command.setAmount(toInteger("amount", element));
        }
        return command;
    }
        
    private Object buildVolumeUpCommand(Element element) throws URISyntaxException {

        return new VolumeUpCommand();
    }
    
    private Object buildVolumeDownCommand(Element element) throws URISyntaxException {

        return new VolumeDownCommand();
    }
    
    private Object buildSpeedUpCommand(Element element) throws URISyntaxException {

    	return new SpeedUpCommand();
    }
    
    private Object buildSpeedDownCommand(Element element) throws URISyntaxException {

    	return new SpeedDownCommand();
    }    
    
    // Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Output) {
            createOutput((Output) object, document);
        } else if (object instanceof PauseCommand) {
            createPauseCommand((PauseCommand) object, document);
        } else if (object instanceof ResumeCommand) {
            createResumeCommand((ResumeCommand) object, document);
        } else if (object instanceof SeekCommand) {
            createSeekCommand((SeekCommand) object, document);
        } else if (object instanceof SpeedDownCommand) {
            createSpeedDownCommand((SpeedDownCommand) object, document);
        } else if (object instanceof SpeedUpCommand) {
            createSpeedUpCommand((SpeedUpCommand) object, document);
        } else if (object instanceof VolumeUpCommand) {
            createVolumeUpCommand((VolumeUpCommand) object, document);
        } else if (object instanceof VolumeDownCommand) {
            createVolumeDownCommand((VolumeDownCommand) object, document);
        } else if (object instanceof OutputCompleteEvent) {
            createOutputCompleteEvent((OutputCompleteEvent) object, document);
        }
    }
    
    private void createOutput(Output output, Document document) throws Exception {
    	
        Element root = document.addElement(new QName("output", NAMESPACE));
        
        if (output.getBargeinType() != null ) {
            root.addAttribute("interrupt-on", output.getBargeinType().name().toLowerCase());        	
        }
        if (output.getStartOffset() != null ) {
            root.addAttribute("start-offset", String.valueOf(output.getStartOffset().getMillis()));        	
        }
        if (output.isStartPaused() != null ) {
            root.addAttribute("start-paused", String.valueOf(output.isStartPaused()));          
        }
        if (output.getRepeatInterval() != null ) {
            root.addAttribute("repeat-interval", String.valueOf(output.getRepeatInterval().getMillis()));         
        }
        if (output.getRepeatTimes() != null ) {
            root.addAttribute("repeat-times", String.valueOf(output.getRepeatTimes()));         
        }
        if (output.getMaxTime() != null ) {
            root.addAttribute("max-time", String.valueOf(output.getMaxTime().getMillis()));         
        }
        if (output.getVoice() != null ) {
            root.addAttribute("voice", output.getVoice());        	
        }
        if (output.getPrompt() != null) {
            addSsml(output.getPrompt(), root);
        }
    }

    private void createPauseCommand(PauseCommand command, Document document) throws Exception {
        document.addElement(new QName("pause", NAMESPACE));
    }

    private void createResumeCommand(ResumeCommand command, Document document) throws Exception {
        document.addElement(new QName("resume", NAMESPACE));
    }
    
    private void createSeekCommand(SeekCommand command, Document document) throws Exception {
        
    	Element seek = document.addElement(new QName("seek", NAMESPACE));
    	seek.addAttribute("amount", String.valueOf(command.getAmount()));
    	seek.addAttribute("direction", command.getDirection().toString());    	
    }
    
    private void createSpeedUpCommand(SpeedUpCommand command, Document document) throws Exception {
        
    	document.addElement(new QName("speed-up", NAMESPACE));
    }
    
    private void createSpeedDownCommand(SpeedDownCommand command, Document document) throws Exception {
        
    	document.addElement(new QName("speed-down", NAMESPACE));
    }
    
    private void createVolumeUpCommand(VolumeUpCommand command, Document document) throws Exception {
        
    	document.addElement(new QName("volume-up", NAMESPACE));
    }
    
    private void createVolumeDownCommand(VolumeDownCommand command, Document document) throws Exception {
        
    	document.addElement(new QName("volume-down", NAMESPACE));
    }
    
    private void createOutputCompleteEvent(OutputCompleteEvent event, Document document) throws Exception {
        addCompleteElement(document, event, COMPLETE_NAMESPACE);
    }

    protected BargeinType loadBargeinType(Element element) {
        try {
            return BargeinType.valueOf(element.attributeValue("interrupt-on").toUpperCase());
        } catch (Exception e) {
            throw new ValidationException(Messages.INVALID_BARGEIN_TYPE);
        }
    }

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Output.class || 
               clazz == PauseCommand.class || 
               clazz == ResumeCommand.class || 
               clazz == VolumeUpCommand.class || 
               clazz == VolumeDownCommand.class || 
               clazz == SeekCommand.class || 
               clazz == SpeedUpCommand.class || 
               clazz == SpeedDownCommand.class || 
               clazz == OutputCompleteEvent.class;
    }
}
