package no.nav.fo.veilarbdialog.rest;


import org.glassfish.jersey.server.ResourceConfig;

public class RestConfig extends ResourceConfig {

    public RestConfig() {
        super(
                JsonProvider.class,
                RestService.class
        );
    }

}