package org.openmrs.projectbuendia.webservices.rest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.fail;

/** Base class for resource tests that issue simulated REST API requests. */
public abstract class BaseApiRequestTest extends MainResourceControllerTest {
    private static boolean VERBOSE = true;

    protected EncounterService encounterService;
    protected LocationService locationService;
    protected OrderService orderService;
    protected PatientService patientService;
    protected ProviderService providerService;

    @Override public String getNamespace() {
        return "buendia";
    }

    /** Returns the path of the resource, under the path "/buendia". */
    public abstract String getURI();

    /** The expected number of records returned by a GET request to the main resource path. */
    public abstract long getAllCount();

    /** Returns a UUID for testing basic GET queries (only to verify a non-error, non-null result). */
    public abstract String getUuid();

    /** A list of the data files to preload with executeDataSet() before every test. */
    public abstract String[] getInitialDataFiles();

    @Rule public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            if (VERBOSE) System.err.println("=== Starting " + description.getMethodName() + "()");
        }
        protected void succeeded(Description description) {
            if (VERBOSE) System.err.println("\u001b[32;1m=== PASSED: " + description.getMethodName() + "()\u001b[0m");
        }
        protected void failed(Description description) {
            if (VERBOSE) System.err.println("\u001b[31;1m=== FAILED: " + description.getMethodName() + "()\u001b[0m");
        }
    };

    /**
     * {@link BaseModuleContextSensitiveTest} does this initialization, but also pre-loads
     * the database with a bunch of records. We don't want to load those records, because
     * we'd then have to augment them with `buendia_[type]_sync_map` records, which would
     * couple our test integrity to the records in OpenMRS' test data. For this reason, we disable
     * {@link BaseModuleContextSensitiveTest}'s setup by putting the {@link SkipBaseSetup}
     * annotation on the class, but then we've got to explicitly init the database and
     * authenticate ourselves.
     */
    @Before public void setUp() throws Exception {
        Logger.SILENT = !VERBOSE;

        encounterService = Context.getEncounterService();
        locationService = Context.getLocationService();
        orderService = Context.getOrderService();
        patientService = Context.getPatientService();
        providerService = Context.getProviderService();

        if (useInMemoryDatabase()) {
            initializeInMemoryDatabase();
            authenticate();
        }
        for (String path : getInitialDataFiles()) {
            executeDataSet(path);
        }
    }

    protected void assertExceptionOnRequest(HttpServletRequest request, String reason) {
        try {
            handle(request);
            fail("Exception due to " + reason + " was not thrown as expected");
        } catch (Exception expected) {
            if (VERBOSE) System.err.println(expected.getClass().getName()
                + " due to " + reason + " was thrown as expected");
        }
    }
}
