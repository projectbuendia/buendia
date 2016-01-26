package org.openmrs.module.sync.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.TestUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.ui.ModelMap;

/**
 * Tests for the {@link ConfigCurrentServerFormController}
 */
@Controller
public class ViewRecordControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	
	@Test
	@NotTransactional
	public void shouldSetAllSyncServerRecordsToNotToSync() throws Exception {

		// set up the basic test data, need to configure as a parent server so sync_server_records are created
		deleteAllData();
        try {
            executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
		executeDataSet("org/openmrs/module/sync/include/SyncParentServer.xml");
		authenticate();
		
		// first, let's change some Person information so that the system creates a sync record
		PersonService ps = Context.getPersonService();
		PersonAttributeType type = ps.getPersonAttributeType(1);
		Person person = ps.getPerson(4);
		person.addAttribute(new PersonAttribute(type, "name")); // assign an arbitrary string value to this attribute 
		ps.savePerson(person);
		
		// now fetch the first sync record in the queue
		SyncService syncService = Context.getService(SyncService.class);
		SyncRecord syncRecord = syncService.getFirstSyncRecordInQueue();
		
		// now mimic controller remove functionality
		ViewRecordController viewRecordController = new ViewRecordController();
		viewRecordController.showThePage(new ModelMap(), null, syncRecord.getUuid(), "remove");
		
		// this record and it's server records should now be set not to sync
		Assert.assertEquals(SyncRecordState.NOT_SUPPOSED_TO_SYNC, syncRecord.getState());
		for (SyncServerRecord syncServerRecord : syncRecord.getServerRecords()){
			Assert.assertEquals(SyncRecordState.NOT_SUPPOSED_TO_SYNC, syncServerRecord.getState());
		}
	}
	
	
	@Test
	@NotTransactional
	public void shouldSetAllSyncServerRecordsToNew() throws Exception {

		// set up the basic test data, need to configure as a parent server so sync_server_records are created
		deleteAllData();
        try {
            executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
		executeDataSet("org/openmrs/module/sync/include/SyncParentServer.xml");
		authenticate();
		
		// first, let's change some Person information so that the system creates a sync record
		PersonService ps = Context.getPersonService();
		PersonAttributeType type = ps.getPersonAttributeType(1);
		Person person = ps.getPerson(4);
		person.addAttribute(new PersonAttribute(type, "name")); // assign an arbitrary string value to this attribute 
		ps.savePerson(person);
		
		// now fetch the first sync record in the queue
		SyncService syncService = Context.getService(SyncService.class);
		SyncRecord syncRecord = syncService.getFirstSyncRecordInQueue();
		
		// now mimic controller remove functionality
		ViewRecordController viewRecordController = new ViewRecordController();
		viewRecordController.showThePage(new ModelMap(), null, syncRecord.getUuid(), "remove");
		
		// then mimic controller reset functionality
		viewRecordController.showThePage(new ModelMap(), null, syncRecord.getUuid(), "reset");
		
		// this record and it's server records should now be set not to sync
		Assert.assertEquals(SyncRecordState.NEW, syncRecord.getState());
		for (SyncServerRecord syncServerRecord : syncRecord.getServerRecords()){
			Assert.assertEquals(SyncRecordState.NEW, syncServerRecord.getState());
		}
	}
}
