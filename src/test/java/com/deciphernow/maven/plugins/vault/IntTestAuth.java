package com.deciphernow.maven.plugins.vault;

import com.bettercloud.vault.VaultException;
import com.deciphernow.maven.plugins.vault.config.Authentication;
import com.deciphernow.maven.plugins.vault.config.Mapping;
import com.deciphernow.maven.plugins.vault.config.Path;
import com.deciphernow.maven.plugins.vault.config.Server;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class IntTestAuth {

    private static final URL VAULT_CERTIFICATE = IntTestVaults.class.getResource("certificate.pem");
    private static final String VAULT_HOST = System.getProperty("vault.host", "localhost");
    private static final String VAULT_PORT = System.getProperty("vault.port", "443");
    private static final String VAULT_SERVER = String.format("https://%s:%s", VAULT_HOST, VAULT_PORT);
    private static final Map<String,String> VAULT_GITHUB_AUTH = Map.of(Authentication.GITHUB_TOKEN_TAG, System.getProperty("vault.github.token"));

    private static Mapping randomMapping() {
        return new Mapping(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private static List<Mapping> randomMappings(int count) {
        return IntStream.range(0, count).mapToObj(i -> randomMapping()).collect(Collectors.toList());
    }

    private static Path randomPath(int mappingCount) {
        return new Path(String.format("secret/%s", UUID.randomUUID().toString()), randomMappings(mappingCount));
    }

    private static List<Path> randomPaths(int pathCount, int mappingCount) {
        return IntStream.range(0, pathCount).mapToObj(i -> randomPath(mappingCount)).collect(Collectors.toList());
    }

    private static class Fixture {

        private final List<Server> servers;
        private final Properties properties;

        private Fixture() throws URISyntaxException {
            List<Path> paths = randomPaths(10, 10);
            File certificate = new File(VAULT_CERTIFICATE.toURI());
            this.servers = ImmutableList.of(new Server(VAULT_SERVER, null, true, certificate, VAULT_GITHUB_AUTH, "", paths, false, 1));
            this.properties = new Properties();
            this.servers.stream().forEach(server -> {
                server.getPaths().stream().forEach(path -> {
                    path.getMappings().stream().forEach(mapping -> {
                        this.properties.setProperty(mapping.getProperty(), UUID.randomUUID().toString());
                    });
                });
            });
        }

        private static void with(Consumer<Fixture> test) throws URISyntaxException {
            test.accept(new IntTestAuth.Fixture());
        }

    }

    private VaultMojo mojoStub = new VaultMojo() {
        @Override
        void executeVaultOperation() {
            getLog().info("execution ended successfully");
        }
    };

    @Test
    public void testVaultAuthentication() throws URISyntaxException {
        IntTestAuth.Fixture.with(fixture -> {
            mojoStub.project = new MavenProject();
            mojoStub.servers = fixture.servers;
            mojoStub.skipExecution = false;
            try {
                mojoStub.execute();
                mojoStub.servers.stream().forEach(server -> assertNotNull(server.getToken()));
            } catch (MojoExecutionException exception) {
                fail(String.format("Unexpected exception while executing: %s", exception.getMessage()));
            }
        });
    }


    /**
     * Tests the {@link PushMojo#execute()} method.
     *
     * @throws URISyntaxException if an exception is raised parsing the certificate
     */
    @Test
    public void testAuthenticatedPushExecute() throws URISyntaxException {
        IntTestAuth.Fixture.with(fixture -> {
            PushMojo mojo = new PushMojo();
            mojo.project = new MavenProject();
            mojo.servers = fixture.servers;
            mojo.skipExecution = false;
            fixture.properties.stringPropertyNames().stream().forEach(key -> {
                mojo.project.getProperties().setProperty(key, fixture.properties.getProperty(key));
            });
            Properties properties = new Properties();
            try {
                mojo.execute();
                Vaults.pull(fixture.servers, properties);
                assertTrue(Maps.difference(fixture.properties, mojo.project.getProperties()).areEqual());
            } catch (MojoExecutionException exception) {
                fail(String.format("Unexpected exception while executing: %s", exception.getMessage()));
            } catch (VaultException exception) {
                fail(String.format("Unexpected exception while pushing to Vault: %s", exception.getMessage()));
            }
        });
    }


    @Test
    public void testAuthenticatedPullExecute() throws URISyntaxException {
        IntTestAuth.Fixture.with(fixture -> {
            PullMojo mojo = new PullMojo();
            mojo.project = new MavenProject();
            mojo.servers = fixture.servers;
            mojo.skipExecution = false;
            try {
                Vaults.authenticateIfNecessary(fixture.servers);
                Vaults.push(fixture.servers, fixture.properties);
                mojo.execute();
                assertTrue(Maps.difference(fixture.properties, mojo.project.getProperties()).areEqual());
            } catch (MojoExecutionException exception) {
                fail(String.format("Unexpected exception while executing: %s", exception.getMessage()));
            } catch (VaultException exception) {
                fail(String.format("Unexpected exception while pushing to Vault: %s", exception.getMessage()));
            }
        });
    }
}

