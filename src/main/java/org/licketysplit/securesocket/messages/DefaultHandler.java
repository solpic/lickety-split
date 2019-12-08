package org.licketysplit.securesocket.messages;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This defines the annotation @DefaultHandler(type = SomeMessage.class).
 * All classes annotated as such are loaded into a map at runtime for later usage.
 * When a message is sent to a peer that is not a response, i.e. it is an
 * unsolicited message, a default handler is used to handle the message.
 * For example, if Peer A sends a ChunkDownloadRequest message to peer B,
 * Peer B will look into its map of default handlers and find the default
 * handler for a message of type ChunkDownloadRequest. Peer B will then
 * instantiate an object of that type and call its handle method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultHandler {
    /**
     * The Message class that this is a default handler for.
     *
     * @return the class
     */
    Class type();
}