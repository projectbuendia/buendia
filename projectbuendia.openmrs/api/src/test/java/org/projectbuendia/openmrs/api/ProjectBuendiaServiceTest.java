package org.projectbuendia.openmrs.api;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.projectbuendia.openmrs.api.ProjectBuendiaService;

/**
 * Tests {@link ${ProjectBuendiaService}}.
 */
public class ProjectBuendiaServiceTest extends BaseModuleContextSensitiveTest {
	
	@Test
	public void shouldSetupContext() {
		assertNotNull(Context.getService(ProjectBuendiaService.class));
	}
}
