package com.tropo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidationException;
import com.tropo.core.verb.JumpCommand;
import com.tropo.core.verb.MoveCommand;
import com.tropo.core.verb.Output;
import com.tropo.core.verb.OutputCompleteEvent;
import com.tropo.core.verb.OutputCompleteEvent.Reason;
import com.tropo.core.verb.PauseCommand;
import com.tropo.core.verb.ResumeCommand;
import com.tropo.core.verb.SpeedCommand;
import com.tropo.core.verb.VolumeCommand;

public class OutputProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:rayo:output:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:output:complete:1");

    private static final QName PAUSE_QNAME = new QName("pause", NAMESPACE);
    private static final QName RESUME_QNAME = new QName("resume", NAMESPACE);
    private static final QName JUMP_QNAME = new QName("jump", NAMESPACE);
    private static final QName MOVE_QNAME = new QName("move", NAMESPACE);
    private static final QName SPEED_QNAME = new QName("speed", NAMESPACE);
    private static final QName VOLUME_QNAME = new QName("volume", NAMESPACE);
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("output")) {
            return buildOutput(element);
        } else if (PAUSE_QNAME.equals(element.getQName())) {
            return buildPauseCommand(element);
        } else if (RESUME_QNAME.equals(element.getQName())) {
            return buildResumeCommand(element);
        } else if (JUMP_QNAME.equals(element.getQName())) {
            return buildJumpCommand(element);
        } else if (MOVE_QNAME.equals(element.getQName())) {
            return buildMoveCommand(element);
        } else if (SPEED_QNAME.equals(element.getQName())) {
            return buildSpeedCommand(element);
        } else if (VOLUME_QNAME.equals(element.getQName())) {
            return buildVolumeCommand(element);
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
        
        if (element.attribute("bargein") != null) {
        	output.setBargein(toBoolean("bargein", element));
        }
        if (element.attribute("codec") != null) {
        	output.setCodec(element.attributeValue("codec"));
        }
        if (element.attribute("format") != null) {
        	output.setFormat(element.attributeValue("format"));
        }
        if (element.attribute("jump-playlist-increment") != null) {
        	output.setJumpPlaylistIncrement(toInteger("jump-playlist-increment",element));
        }
        if (element.attribute("jump-time") != null) {
        	output.setJumpTime(toInteger("jump-time",element));
        }
        if (element.attribute("offset") != null) {
        	output.setOffset(toInteger("offset", element));
        }
        if (element.attribute("repeat-times") != null) {
        	output.setRepeatTimes(toInteger("repeat-times", element));
        }
        if (element.attribute("start-in-pause-mode") != null) {
        	output.setStartInPauseMode(toBoolean("start-in-pause-mode", element));
        }
        if (element.attribute("timeout") != null) {
        	output.setTimeout(toInteger("timeout", element));
        }
        if (element.attribute("volume-unit") != null) {
        	output.setVolumeUnit(toInteger("volume-unit", element));
        }
        return output;
    }

    private Object buildPauseCommand(Element element) {
        return new PauseCommand();
    }

    private Object buildResumeCommand(Element element) {
        return new ResumeCommand();
    }

    private Object buildJumpCommand(Element element) {
        
    	JumpCommand command = new JumpCommand();
        if (element.attribute("position") != null) {
        	try {
        		command.setPosition(Integer.parseInt(element.attributeValue("position")));
        	} catch (ValidationException e) {
        		throw new ValidationException(Messages.INVALID_POSITION);
        	}
        }
        return command;
    }
    
    private Object buildMoveCommand(Element element) throws URISyntaxException {

    	MoveCommand command = new MoveCommand();
        if (element.attribute("time") != null) {
        	try {
        		command.setTime(Integer.parseInt(element.attributeValue("time")));
        	} catch (ValidationException e) {
        		throw new ValidationException(Messages.INVALID_TIME);
        	}
        }
        if (element.attribute("direction") != null) {
        	try {
        		command.setDirection(Boolean.parseBoolean(element.attributeValue("direction")));
        	} catch (ValidationException e) {
        		throw new ValidationException(Messages.INVALID_DIRECTION);
        	}
        }
        return command;
    }
    
    
    private Object buildVolumeCommand(Element element) throws URISyntaxException {

    	VolumeCommand command = new VolumeCommand();
        if (element.attribute("up") != null) {
        	try {
        		command.setUp(Boolean.parseBoolean(element.attributeValue("up")));
        	} catch (ValidationException e) {
        		throw new ValidationException(Messages.INVALID_VOLUME);
        	}
        }
        return command;
    }
    
    private Object buildSpeedCommand(Element element) throws URISyntaxException {

    	SpeedCommand command = new SpeedCommand();
        if (element.attribute("up") != null) {
        	try {
        		command.setUp(Boolean.parseBoolean(element.attributeValue("up")));
        	} catch (ValidationException e) {
        		throw new ValidationException(Messages.INVALID_SPEED);
        	}
        }
        return command;
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
        } else if (object instanceof MoveCommand) {
            createMoveCommand((MoveCommand) object, document);
        } else if (object instanceof SpeedCommand) {
            createSpeedCommand((SpeedCommand) object, document);
        } else if (object instanceof VolumeCommand) {
            createVolumeCommand((VolumeCommand) object, document);
        } else if (object instanceof JumpCommand) {
            createJumpCommand((JumpCommand) object, document);
        } else if (object instanceof OutputCompleteEvent) {
            createOutputCompleteEvent((OutputCompleteEvent) object, document);
        }
    }
    
    private void createOutput(Output output, Document document) throws Exception {
    	
        Element root = document.addElement(new QName("output", NAMESPACE));
        if (output.getPrompt() != null) {
            addSsml(output.getPrompt(), root);
        }
        if (output.getCodec() != null ) {
        	root.addAttribute("codec", output.getCodec());        	
        }
        if (output.getFormat() != null ) {
        	root.addAttribute("format", output.getFormat());        	
        }
        if (output.getJumpPlaylistIncrement() != null ) {
        	root.addAttribute("jump-playlist-increment", String.valueOf(output.getJumpPlaylistIncrement()));        	
        }
        if (output.getJumpTime() != null ) {
        	root.addAttribute("jump-time", String.valueOf(output.getJumpTime()));        	
        }
        if (output.getOffset() != null ) {
        	root.addAttribute("offset", String.valueOf(output.getOffset()));        	
        }
        if (output.getRepeatTimes() != null ) {
        	root.addAttribute("repeat-times", String.valueOf(output.getRepeatTimes()));        	
        }
        if (output.getTimeout() != null ) {
        	root.addAttribute("timeout", String.valueOf(output.getTimeout()));        	
        }
        if (output.getVoice() != null ) {
        	root.addAttribute("voice", output.getVoice());        	
        }
        if (output.getVolumeUnit() != null ) {
        	root.addAttribute("volume-unit", String.valueOf(output.getVolumeUnit()));        	
        }
        if (output.isBargein() != null ) {
        	root.addAttribute("bargein", String.valueOf(output.isBargein()));        	
        }
        if (output.isStartInPauseMode() != null ) {
        	root.addAttribute("start-in-pause-mode", String.valueOf(output.isStartInPauseMode()));        	
        }
    }

    private void createPauseCommand(PauseCommand command, Document document) throws Exception {
        document.addElement(new QName("pause", NAMESPACE));
    }

    private void createResumeCommand(ResumeCommand command, Document document) throws Exception {
        document.addElement(new QName("resume", NAMESPACE));
    }

    private void createJumpCommand(JumpCommand command, Document document) throws Exception {
        
    	Element jump = document.addElement(new QName("jump", NAMESPACE));
    	jump.addAttribute("position", String.valueOf(command.getPosition()));
    }
    
    private void createMoveCommand(MoveCommand command, Document document) throws Exception {
        
    	Element move = document.addElement(new QName("move", NAMESPACE));
    	move.addAttribute("time", String.valueOf(command.getTime()));
    	move.addAttribute("direction", String.valueOf(command.isDirection()));    	
    }
    
    private void createSpeedCommand(SpeedCommand command, Document document) throws Exception {
        
    	Element move = document.addElement(new QName("speed", NAMESPACE));
    	move.addAttribute("up", String.valueOf(command.isUp()));    	
    }
    
    private void createVolumeCommand(VolumeCommand command, Document document) throws Exception {
        
    	Element move = document.addElement(new QName("volume", NAMESPACE));
    	move.addAttribute("up", String.valueOf(command.isUp()));    	
    }
    
    private void createOutputCompleteEvent(OutputCompleteEvent event, Document document) throws Exception {
        addCompleteElement(document, event, COMPLETE_NAMESPACE);
    }

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Output.class || 
               clazz == PauseCommand.class || 
               clazz == ResumeCommand.class || 
               clazz == VolumeCommand.class || 
               clazz == JumpCommand.class || 
               clazz == MoveCommand.class || 
               clazz == SpeedCommand.class || 
               clazz == OutputCompleteEvent.class;
    }
}
