package com.appMobi.appMobiLib.oauth;

public class Service {
	public String name, appKey, secret, requestTokenEndpoint, authorizeEndpoint, accessTokenEndpoint, verb;
	
    @Override
	public String toString(){
         return 
         "name: " + name + "\n" +
         "\tappKey: " + appKey + "\n" +
         "\tsecret: " + secret + "\n" +
         "\trequestTokenEndpoint: " + requestTokenEndpoint + "\n" +
         "\tauthorizeEndpoint: " + authorizeEndpoint + "\n" +
         "\tverb: " + verb + "\n" +
         "\taccessTokenEndpoint: " + accessTokenEndpoint;
     }
	
}
