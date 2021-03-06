package edu.thinktank.togglz;

import io.dropwizard.Configuration;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.junit.Test;
import org.togglz.console.TogglzConsoleServlet;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.mem.InMemoryStateRepository;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AbstractFeatureToggleBundleTest {

    @Test
    public void buildFeatureManager() throws Exception {
        final FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig();
        featureToggleConfig.setFeatureSpec(TestFeature.class.getCanonicalName());
        final AbstractFeatureToggleBundle<Configuration> featureToggleBundle = createDefaultBundle(null, null);

        final FeatureManager featureManager = featureToggleBundle.buildFeatureManager(featureToggleConfig);

        assertThat(featureManager.getFeatures()).contains(TestFeature.TEST_FEATURE);
        assertThat(featureManager.getCurrentFeatureUser().getName()).isEqualTo("admin");
        assertThat(featureManager.getCurrentFeatureUser().isFeatureAdmin()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingFeatureSpecWillThrowExceptionWhileBuildingFeatureManager() throws Exception {
        final FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig();
        final AbstractFeatureToggleBundle<Configuration> featureToggleBundle = createDefaultBundle(null, null);

        final FeatureManager featureManager = featureToggleBundle.buildFeatureManager(featureToggleConfig);
    }

    @Test
    public void overrideFeatureSettings() throws Exception {
        final AbstractFeatureToggleBundle<Configuration> featureToggleBundle = createDefaultBundle(null, null);
        final InMemoryStateRepository stateRepository = new InMemoryStateRepository();
        final Feature feature = new Feature() {
            public String name() {
                return "feature";
            }
        };
        stateRepository.setFeatureState(new FeatureState(feature, true));
        final FeatureManager featureManager = new FeatureManagerBuilder().stateRepository(stateRepository).featureEnum(TestFeature.class).build();
        final Map<String, Boolean> featureStatesOverride = new HashMap<>(1);
        featureStatesOverride.put("feature", false);

        assertThat(featureManager.getFeatureState(feature).isEnabled()).isTrue();

        featureToggleBundle.overrideFeatureStatesFromConfig(featureManager, featureStatesOverride);

        assertThat(featureManager.getFeatureState(feature).isEnabled()).isFalse();
    }

    @Test
    public void addServletToAdminContext() throws Exception {
        final Environment environment = mock(Environment.class);
        final AdminEnvironment admin = mock(AdminEnvironment.class, RETURNS_DEEP_STUBS);
        when(environment.admin()).thenReturn(admin);
        final AbstractFeatureToggleBundle<Configuration> featureToggleBundle = createDefaultBundle(null, null);

        featureToggleBundle.addServlet(new FeatureToggleConfig(), environment);

        verify(admin).addServlet("togglz", TogglzConsoleServlet.class);
    }

    @Test
    public void addServletToApplicationContext() throws Exception {
        final Environment environment = mock(Environment.class);
        final AdminEnvironment adminMock = mock(AdminEnvironment.class);
        when(environment.admin()).thenReturn(adminMock);
        final ServletEnvironment servletMock = mock(ServletEnvironment.class, RETURNS_DEEP_STUBS);
        when(environment.servlets()).thenReturn(servletMock);
        final AbstractFeatureToggleBundle<Configuration> featureToggleBundle = createDefaultBundle(null, null);

        final FeatureToggleConfig config = spy(new FeatureToggleConfig());
        when(config.isServletContextAdmin()).thenReturn(false);

        featureToggleBundle.addServlet(config, environment);

        verifyZeroInteractions(adminMock);
        verify(servletMock).addServlet("togglz", TogglzConsoleServlet.class);
    }

    @Test
    public void defaultStateRepositoryShouldBeInMemmory() throws Exception {
        final AbstractFeatureToggleBundle<Configuration> bundle = createDefaultBundle(null, null);

        assertThat(bundle.getStateRepository()).isInstanceOf(InMemoryStateRepository.class);
    }

    @Test
    public void initMethodShouldDoNothing() throws Exception {
        final AbstractFeatureToggleBundle<Configuration> defaultBundle = createDefaultBundle(null, null);
        Bootstrap<Configuration> bootstrapMock = mock(Bootstrap.class);
        defaultBundle.initialize(bootstrapMock);
        verifyZeroInteractions(bootstrapMock);
    }

    private AbstractFeatureToggleBundle<Configuration> createDefaultBundle(final FeatureToggleConfig featureToggleConfig, final StateRepository stateRepository) {
        return new AbstractFeatureToggleBundle<Configuration>() {
            @Override
            public FeatureToggleConfig getBundleConfiguration(final Configuration configuration) {
                return featureToggleConfig;
            }

            @Override
            public StateRepository getStateRepository() {
                return stateRepository != null ? stateRepository : super.getStateRepository();
            }
        };
    }
}