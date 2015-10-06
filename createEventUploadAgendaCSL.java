import lotus.domino.*;
import lotus.domino.Document;

import com.granicus.encoder.EncoderClient;
import com.granicus.soap.*;
import com.granicus.xsd.*;

import java.io.*;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

//Updated 23Sep20.15 by SK - replaced all instances of Chambers with Chamber per 2016.CEHOU.005 References to CHAMBERS checklist
public class JavaAgent extends AgentBase {

	public void NotesMain() {

		try {
			Session session = getSession();
			AgentContext agentContext = session.getAgentContext();
			Agent agent = agentContext.getCurrentAgent();
			Database db = agentContext.getCurrentDatabase();
			lotus.domino.Document doc = (lotus.domino.Document) (db.getDocumentByID(agent.getParameterDocID()));

			PlatformClient client = new PlatformClient("coloradoga.granicus.com", "USERNAME", "PASSWORD");
			System.out.println("gAgtAction=" +doc.getItemValueString("gAgtAction"));
			if (doc.getItemValueString("gAgtAction").equals("createevent"))
			{
				System.out.println("In createevent1");
				createEvent(session, doc, client);
				System.out.println("In createevent2");
			}
			else if (doc.getItemValueString("gAgtAction").equals("uploadagenda"))
				uploadAgenda(doc, client);
			else if (doc.getItemValueString("gAgtAction").equals("verifyEvent"))
			{
				session.setEnvironmentVar("verifyComm","");
				session.setEnvironmentVar("verifyLocation","");
				session.setEnvironmentVar("verifyDateTime","");
				verifyEvent(session, doc, client);
			}
			else if (doc.getItemValueString("gAgtAction").equals("updateEvent"))
				updateEvent(session, doc, client);

			System.out.println("End of Agent");
		} catch (Exception e) {
			System.out.println("Error");
			e.printStackTrace();
		}
	}

	
	
	
	private void createEvent(Session session, lotus.domino.Document doc, PlatformClient client) 
			throws NumberFormatException, NotesException, ParseException, RemoteException {
		System.out.println("Create Event");
		EventData newevent = new EventData();
		System.out.println("comm=" + doc.getItemValueString("Comm"));
		System.out.println("room=" + doc.getItemValueString("gEventRoom"));
		System.out.println("folder=" + doc.getItemValueString("gEventFolder"));
		if (doc.getItemValueString("PrintChamber").equalsIgnoreCase("Yes")) {
			newevent.setName(doc.getItemValueString("Chamber") + " " + doc.getItemValueString("Comm")); // newevent.setName("zzz test Press Room...KAS");
		} else {
			System.out.println("doc.getItemValueString(Comm)" + doc.getItemValueString("Comm"));
			newevent.setName(doc.getItemValueString("Comm")); // newevent.setName("zzz test Press Room...KAS");			
		}
		System.out.println("test 1");
		newevent.setCameraID(Integer.parseInt(doc.getItemValueString("gEventRoom"))); // newevent.setCameraID(6);  // //Press Room
		System.out.println("test 2");

		newevent.setFolderID(Integer.parseInt(doc.getItemValueString("gEventFolder"))); // newevent.setFolderID(56); // //Water Commission
		System.out.println("test 3");

		String commEvent = doc.getItemValueString("CommBillsDate");
		if (commEvent.contains("~")) {
			String[] parts = commEvent.split("~");
			String commDate = parts[0];
			System.out.println("CommDate = " + commDate);
			String commTime = parts[1];
			System.out.println("CommTime = " + commTime);
			if (commTime.equalsIgnoreCase("Upon Adjournment")){
				commTime = "12:00 PM";
			}else if(commTime.indexOf("Upon Adjournment")>= 0){
				commTime = "5:00 PM";
			}
			DateFormat df = new SimpleDateFormat("MM/dd/yyyy h:mm a");
			Date date = df.parse(commDate + " " + commTime);
			System.out.println("df=" + df.toString());
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.MINUTE, 30);
			// Calendar cal = new GregorianCalendar();
			newevent.setMeetingTime(cal);
			newevent.setStartTime(cal);
		
		} else {
			throw new IllegalArgumentException("String " + commEvent + " does not contain ~");
		}
		newevent.setPlayerTemplateID(11);
		newevent.setDuration(36000);
		newevent.setArchiveStatus("Pending");
		newevent.setAutoStart(false);
		newevent.setBroadcast(true);
		newevent.setRecord(true);

		int eventid = client.createEvent(newevent);
		System.out.println("eventid="+eventid);
	//	NotesUIWorkspace ws=new NotesUIWorkspace(); 
	//	NotesUIDocument uidoc=ws.getCurrentDocument(); 
		
		session.setEnvironmentVar("gEventId", eventid);
//		doc.replaceItemValue("gEventId", (Integer)eventid);
	//	doc.save(true, false);
	}

	
	private void uploadAgenda(lotus.domino.Document doc, PlatformClient client) 
			throws IOException, NumberFormatException, NotesException {
		System.out.println("Start of uploadAgenda");
		
		File file = new File(doc.getItemValueString("gAgendaFile")); //"C:\\Users\\manish_jani\\Desktop\\agenda.pdf"); // **
		FileInputStream fin = new FileInputStream(file);
		byte fileContent[] = new byte[(int) file.length()];
		fin.read(fileContent);
		System.out.println("Start of uploadAgenda  3="+doc.getItemValueInteger("gEventId"));
		int eventId = doc.getItemValueInteger("gEventId");
		System.out.println("eventid="+eventId);

		client.uploadEventAgendaDocument(eventId, new com.granicus.xsd.Document("agenda", "",fileContent, "pdf"));

/* Adding agenda items and tagging through CLICS creates duplicate entries in the event, as a result, 
 * I am commenting out the code to create agenda items when uploading agenda and just relying on tagging
 * MJ - 02/06/2014  
 
		// ** set agenda items
		//Notesitem billsList = doc.getFirstItem("CommBillsSelect");
//		System.out.println("Start of uploadAgenda  5");
		String billItem;
		String agdItem;
//		System.out.println("Start of uploadAgenda  6");		
		Vector billsList = doc.getItemValue("CommBillsInfo" + doc.getItemValueString("posCommBillsDate"));
		System.out.println("CommBillsInfo==" + "CommBillsInfo" + doc.getItemValueString("posCommBillsDate"));
//		System.out.println("Start of uploadAgenda  7");
		MetaDataData[] mdata = new MetaDataData[billsList.size()];			
	     for (int i=0; i<billsList.size(); i++) {
	 		mdata[i] = new MetaDataData();
	 		billItem = (String) billsList.elementAt(i);
	 		String[] parts = billItem.split("~");
			String billNum = parts[0];
			String billTitle = parts[1];
			String billSpons = parts[2].replace("^", ", ");
			String billText = parts[4];
			
	 		if (billNum.startsWith("*")) {
	 			mdata[i].setName(billText);
	        } else { 				 		
	 			mdata[i].setName(billNum + ": " + billTitle + "\n (" +  billSpons + ")");
	        }
	 		
			mdata[i].setPayload(new AgendaItem());
	          System.out.println((i+1) + ": " + billItem);
	     }

		client.importEventMetaData(eventId, mdata, true, true);
*/
/*		
		MetaDataData[] mdata = new MetaDataData[2];

		AgendaItem itemdetails = new AgendaItem();
		itemdetails.setDepartment("Finance");
		mdata[0] = new MetaDataData();
		mdata[0].setName("Test Item 1");
		mdata[0].setPayload(itemdetails);

		mdata[1] = new MetaDataData();
		mdata[1].setName("Test Item 2");
		mdata[1].setPayload(new AgendaItem());
		client.importEventMetaData(eventId, mdata, true, true);
*/		
			
		System.out.println("End of uploadAgenda");

	}
	//below is added by EP to check that the agenda has been uploaded in Granicus
	private void verifyEvent(Session session, lotus.domino.Document doc, PlatformClient client) 
	throws IOException, NumberFormatException, NotesException {	
		System.out.println("In verifyEvent");
		
		Hashtable<Integer, String> cameraRoom = new Hashtable<Integer, String>();
		cameraRoom.put(7,"HCR 0107");
		cameraRoom.put(2,"HCR 0109");
		cameraRoom.put(4,"HCR 0112");
		cameraRoom.put(14,"House Chamber");
		cameraRoom.put(15,"Joint Budget Committee");
		cameraRoom.put(9,"LSB - A");
		cameraRoom.put(3,"LSB - B");
		cameraRoom.put(16,"Old Supreme Court Chamber");
		cameraRoom.put(13,"Press Room");
		cameraRoom.put(6,"RM 271");
		cameraRoom.put(12,"SCR 352");
		cameraRoom.put(11,"SCR 353");
		cameraRoom.put(10,"SCR 354");
		cameraRoom.put(5,"SCR 356");
		cameraRoom.put(8,"Senate Chamber");
		
		System.out.println("After hashtable");
		int eventID;
		boolean isCreated = false;
//		boolean timeDateCorrect = false;
//		boolean commCorrect = false;
//		boolean rmCorrect = false;
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
		//getting location from user document (room)
		String userInfoString = doc.getItemValueString("CommBillsDate");
		System.out.println("doc.getItemValueString(CommBills_D)" + doc.getItemValueString("CommBillsDate"));
		System.out.println("userInfoString" + userInfoString);
		String[] userInfoArray = userInfoString.split("~");
		System.out.println(userInfoArray[0]);
		System.out.println(userInfoArray[1]);
		System.out.println(userInfoArray[2]);
		String room = userInfoArray[2];
		//if (room.equals("House Floor")){
		//	room = "House Chamber";
		//}
		//String room = doc.getItemValueString("CommBills_D");
		
		//getting Location from Granicus (cameraID)
		eventID = doc.getItemValueInteger("gEventId");
		System.out.println("gEventId" + eventID);
		EventData event;
		System.out.println("Before event");
		//event = client.getEvent(2014);
		event = client.getEvent(eventID);
		System.out.println("After event");
		String cameraID = "" + event.getCameraID();	
				
		//getting date and committee info from user document (commBillsDate) and (comm)
		String committee = doc.getItemValueString("Chamber") + " " + doc.getItemValueString("comm");
		System.out.println("committee is " + committee);
		String commDateTime = userInfoArray[0] + " " + userInfoArray[1];
		System.out.println("commDateTime is " + commDateTime);
		
		//getting date, time, and committee info from granicus
		String granName = event.getName();
		System.out.println("granName is " + granName);
		Calendar cal = event.getMeetingTime();
		cal.add(Calendar.MINUTE, -30);
		Date halfHrBack = cal.getTime();
		String granDateTime =String.valueOf(df.format(halfHrBack));
		System.out.println("granDateTime is " + granDateTime);
		String granLocationRoom = cameraRoom.get(event.getCameraID());
		System.out.println("granLocationRoom is " + granLocationRoom);
		
		
		// verifying both in console
		System.out.println("cameraID (from the client) is:" + cameraID);//this will give the room number 
		System.out.println("room (from the document) is :" + room);
		
		//checking that the event exists
		if (event != null){
	    	isCreated = true;	    	
		}
		
		String inconsistentData= "false";

		
		//setting granicus values to the global variables for use in the user document
		session.setEnvironmentVar("verifyComm",granName);
		session.setEnvironmentVar("verifyLocation",granLocationRoom);
		session.setEnvironmentVar("verifyDateTime",granDateTime);

		System.out.println("Event status is: " + event.getStatus());
		System.out.println("Event StartTime is: " + event.getStartTime());
		System.out.println("Event meetingTime is: " + event.getMeetingTime());

		
		if(isCreated==true){
			System.out.println("Event Exists!");
			session.setEnvironmentVar("verifyEvent","true");
			//checking if room is correct
			if(!granLocationRoom.equals(room)){				
				System.out.println("Location is incorrect.  Please verify.");
				inconsistentData = "true";
			}
			else if (granLocationRoom.equals(room)){
				System.out.println("Location is correct!");
			}
			//check if datetime is correct
			if(!granDateTime.equals(commDateTime)){
				System.out.println("Date/Time is incorrect.  Please verify.");
				inconsistentData = "true";
			}
			else if(granDateTime.equals(commDateTime)){
				System.out.println("Date/Time is correct!");
			}
			//check if committee is correct			
			if(!granName.equals(committee)){
				System.out.println("Committee does not match.  Please verify.");
				inconsistentData = "true";	
			}
			else if(granName.equals(committee)){
				System.out.println("Committee matches!");
			}
		}
		else if (isCreated==false){
			System.out.println("Event does NOT exist!");
			session.setEnvironmentVar("verifyEvent","false");
		}
		session.setEnvironmentVar("inconsistentData", inconsistentData);
		

		//if(isCreated==true & rmCorrect==true & timeDateCorrect==true & commCorrect==true){
		//	return true;
		//}
	}
	
	// adding an updateEvent function that will update an already existing event in granicus
	private void updateEvent(Session session, lotus.domino.Document doc, PlatformClient client) 
	throws NumberFormatException, NotesException, ParseException, RemoteException {

			System.out.println("Update Event");
			int eventID;
			eventID = doc.getItemValueInteger("gEventId");
			System.out.println("gEventId" + eventID);
			EventData event = new EventData();
			System.out.println("Before event");
			//event = client.getEvent(2014);
			event = client.getEvent(eventID);

			
			System.out.println("event" + event);
			System.out.println("event.getCarmeraID():"+ event.getCameraID());
			event.setCameraID(Integer.parseInt(doc.getItemValueString("gEventRoom"))); // newevent.setCameraID(6);  // //Press Room
			System.out.println("event.getCarmeraID():"+ event.getCameraID());
			event.setFolderID(Integer.parseInt(doc.getItemValueString("gEventFolder"))); // newevent.setFolderID(56); // //Water Commission

			String commEvent = doc.getItemValueString("CommBillsDate");
			if (commEvent.contains("~")) {
				String[] parts = commEvent.split("~");
				System.out.println("commEvent: " + commEvent);
				String commDate = parts[0];
				String commTime = parts[1];
				System.out.println("commDate:" + commDate);
				System.out.println("commTime:" + commTime);
				if (commTime.equalsIgnoreCase("Upon Adjournment")) 	commTime = "12:00 PM";
				DateFormat df = new SimpleDateFormat("MM/dd/yyyy h:mm a");
				Date date = df.parse(commDate + " " + commTime);
				System.out.println("df=" + df.toString());
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.add(Calendar.MINUTE, 30);
				System.out.println("cal" + cal);
				// Calendar cal = new GregorianCalendar();	
				System.out.println("event.getMeetingtime()" + event.getMeetingTime());
				event.setMeetingTime(cal);
				System.out.println("new event.getMeetingtime()" + event.getMeetingTime());
				event.setStartTime(cal);

			} else {
				throw new IllegalArgumentException("String " + commEvent + " does not contain ~");
			}
			event.setPlayerTemplateID(11);
			event.setDuration(36000);
			event.setArchiveStatus("Pending");
			event.setAutoStart(false);
			event.setBroadcast(true);
			event.setRecord(true);
			
			client.updateEvent(event);

			System.out.println("eventid="+eventID);


			session.setEnvironmentVar("gEventId", eventID);

		}
	
}