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
		lotus.domino.Document doc ;
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			System.out.println("AGENT START TIME="+dateFormat.format(new Date()));
			Session session = getSession();
			AgentContext agentContext = session.getAgentContext();
			Agent agent = agentContext.getCurrentAgent();
			Database db = agentContext.getCurrentDatabase();
			try{
				doc = (lotus.domino.Document) (db.getDocumentByID(agent.getParameterDocID()));
			} catch (Exception e) {
				doc = (lotus.domino.Document) (agentContext.getDocumentContext());
			}
			PlatformClient client = new PlatformClient("coloradoga.granicus.com", "USERNAME", "PASSWORD");
			System.out.println("gAgtAction=" +doc.getItemValueString("gAgtAction"));
			
			if (doc.getItemValueString("gAgtAction").equals("pickevent"))
				pickEvent(session, db, doc, client);
			else if (doc.getItemValueString("gAgtAction").equals("loadevent"))
				loadEvent(session, doc, client);
			else if (doc.getItemValueString("gAgtAction").equals("startevent"))
				startEvent(session, doc, client);
			else if (doc.getItemValueString("gAgtAction").equals("pauseEvent"))
				pauseEvent(session, doc, client);
			else if (doc.getItemValueString("gAgtAction").equals("stopevent"))
				stopEvent(session, doc, client);
			else if (doc.getItemValueString("gAgtAction").equals("tagevent"))
				tagEvent(doc, client);
			else if (doc.getItemValueString("gAgtAction").equals("pickStatus"))
				pickStatus(session, doc, client);

			

			System.out.println("AGENT END TIME="+dateFormat.format(new Date()));

		} catch (Exception e) {
			System.out.println("Error");
			e.printStackTrace();
		}
	}

	private void pickStatus(Session session, lotus.domino.Document doc, PlatformClient client) 
	throws NumberFormatException, NotesException, ParseException, RemoteException{
		//below added by EP
		int eventID;
		eventID = doc.getItemValueInteger("gEventId");
		System.out.println("eventID: " + eventID);
		EventData event;
		event = client.getEvent(eventID);
//		System.out.println("After Start");
		System.out.println("client.getEvent(2048)" + event);
		System.out.println("event.getStatus() " + event.getStatus());
		System.out.println("event.getStartTime()" +event.getStartTime());
		System.out.println("event.getState()" +event.getState());
		System.out.println("event.getName()" +event.getName());
		System.out.println("event.getRecord()" +event.getRecord());
		String eventStatus;
		eventStatus = "" + event.getStatus();
		if(eventStatus.equals(""))
			session.setEnvironmentVar("eventStatus", "Not Started");
		else
			session.setEnvironmentVar("eventStatus", eventStatus);
		//above added by EP
	}
	
	
	private void pickEvent(Session session, Database db, lotus.domino.Document doc, PlatformClient client) 
			throws NumberFormatException, NotesException, ParseException, RemoteException {

		System.out.println("Pick Event");
		//creating a hashtable to look up the committee meeting room instead of logging into the client
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
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy h:mm a");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		int cameraID;
		String camera;
		
		Calendar startdt = Calendar.getInstance() ;
//		startdt.add(Calendar.DATE, -1);
		startdt.set(Calendar.HOUR_OF_DAY, 0);
		startdt.set(Calendar.MINUTE, 0);
		startdt.set(Calendar.SECOND, 0);
		startdt.set(Calendar.MILLISECOND, 0);
		
		Calendar enddt = Calendar.getInstance() ;
		enddt.add(Calendar.DATE, 1);
//		enddt.add(Calendar.HOUR_OF_DAY, 3);	
		enddt.set(Calendar.HOUR_OF_DAY, 0);
		enddt.set(Calendar.MINUTE, 0);
		enddt.set(Calendar.SECOND, 0);
		enddt.set(Calendar.MILLISECOND, 0);
		
		System.out.println("Start Date=" + df.format(startdt.getTime()));
		System.out.println("End Date=" + df.format(enddt.getTime()));
		
		Vector<String> eventList = new Vector<String>();
		System.out.println("AGENT BEFORE getEventsByDateRange="+dateFormat.format(new Date()));
		//below takes 5 seconds, find a way to do this on the server
		EventData[] events = client.getEventsByDateRange(startdt, enddt);

		System.out.println("AGENT AFTER getEventsByDateRange="+dateFormat.format(new Date()));

		//try and catch added by EP 11/3/2014 to catch the errors if there are no events in the range
		try{
		for(EventData event : events) {
			// eventList.addElement( String.format("%-25s%-150s%-50s%-50s",
			//		String.valueOf(df.format(event.getStartTime().getTime())), event.getName(),client.getCamera(event.getCameraID()).getName()
			//		, "~" + event.getCameraID() + "^" + event.getUID()));
			System.out.println("1--AGENT before .getCameraID()="+dateFormat.format(new Date()));
			cameraID = event.getCameraID();
			System.out.println("2--AGENT before .getName()"+dateFormat.format(new Date()));	
			//get below in some other way!!
			camera = cameraRoom.get(cameraID);
			//camera = client.getCamera(cameraID).getName();
			

			System.out.println("3--AGENT before .addElement()"+dateFormat.format(new Date()));
			eventList.addElement(
					String.valueOf(df.format(event.getStartTime().getTime()))
					+ " -- " + event.getName()
					+ " -- " + camera
					+ " -- " + event.getID()
					+ "                                         " + "~" + cameraID + "^" + event.getUID() + "^" + camera
					);


			System.out.println("4--AGENT after .addElement()"+dateFormat.format(new Date()));
//			System.out.println(event.getName() + " --- " + event.getCameraID() + " --- " + event.getAgendaType() + " --- " + df.format(event.getStartTime().getTime()) + " --- " + df.format(event.getMeetingTime().getTime()) + " *** " + event.getUID());			
		}
		}catch(Exception e){
			View keywordsVw = db.getView("(vv41001a-dbConfiguration)");
			lotus.domino.Document kywdDoc = (lotus.domino.Document) keywordsVw.getFirstDocument();
			kywdDoc.replaceItemValue("gMeetingList", "");		
	        kywdDoc.save(true, false);
		}
		
		System.out.println("AGENT AFTER for events loop="+dateFormat.format(new Date()));
		
		    for(int index=0; index < eventList.size(); index++)
		      System.out.println("eventlist="+eventList.get(index));
		
		View keywordsVw = db.getView("(vv41001a-dbConfiguration)");
		lotus.domino.Document kywdDoc = (lotus.domino.Document) keywordsVw.getFirstDocument();
		kywdDoc.replaceItemValue("gMeetingList", eventList);		
        kywdDoc.save(true, false);
	}

	private void loadEvent(Session session, lotus.domino.Document doc, PlatformClient client) 
	throws IOException, NumberFormatException, NotesException {
		System.out.println("Start of load recording");		
	
		CameraData cam = client.getCamera(doc.getItemValueInteger("gCameraId"));
		System.out.println(cam.getName() + " ****--- " + cam.getID() +   "--------*****"    + cam.getIdentifier()+ "*******"+ cam.getControlPort());
		
		String intIP = cam.getInternalIP();
		System.out.println("cam.getInternalIP():" + intIP);
		int contPort = cam.getControlPort();
		System.out.println("cam.getControlPort():" + contPort);
		EncoderClient enc = new EncoderClient(intIP,contPort);
		System.out.println("after EncoderClient enc = new EncoderClient");
		//EncoderClient enc = new EncoderClient(cam.getInternalIP(), cam.getControlPort() );
		System.out.println("gEventId" + doc.getItemValueString("gEventId"));
		System.out.println("gMeetingId" + doc.getItemValueString("gMeetingId"));
		enc.loadMeeting(doc.getItemValueString("gMeetingId"));
		
		//below added by EP
		int eventID;
		eventID = doc.getItemValueInteger("gEventId");
		System.out.println("gEventId" + eventID);
		EventData event;
		event = client.getEvent(eventID);
		session.setEnvironmentVar("eventStatus", event.getStatus());
		//above added by EP
		System.out.println("End of load recording");
	}
	
	private void startEvent(Session session, lotus.domino.Document doc, PlatformClient client) 
			throws IOException, NumberFormatException, NotesException {
		System.out.println("Start of start recording");

		CameraData cam = client.getCamera(doc.getItemValueInteger("gCameraId"));
		System.out.println(cam.getName() + " ****--- " + cam.getID() +   "--------*****"    + cam.getIdentifier()+ "*******"+ cam.getControlPort());
		EncoderClient enc = new EncoderClient(cam.getInternalIP(), cam.getControlPort() );

//		enc.loadMeeting(doc.getItemValueString("gMeetingId"));
		enc.startMeeting();
		
		//below added by EP
		int eventID;
		eventID = doc.getItemValueInteger("gEventId");
		EventData event;
		event = client.getEvent(eventID);
		session.setEnvironmentVar("eventStatus", event.getStatus());
		//above added by EP

		System.out.println("End of start recording");
	}
	
	private void pauseEvent(Session session, lotus.domino.Document doc, PlatformClient client) 
	throws IOException, NumberFormatException, NotesException {
		System.out.println("Start of pause recording");
		
		CameraData cam = client.getCamera(doc.getItemValueInteger("gCameraId"));
		System.out.println(cam.getName() + " ****--- " + cam.getID() +   "--------*****"    + cam.getIdentifier()+ "*******"+ cam.getControlPort());
		EncoderClient enc = new EncoderClient(cam.getInternalIP(), cam.getControlPort() );
		
		//grancius does not have a pause method that can be called on the encoder, need to look into this and fix the code below
		//enc.pauseMeeting();
		
		//below added by EP
		int eventID;
		eventID = doc.getItemValueInteger("gEventId");
		EventData event;
		event = client.getEvent(eventID);
		session.setEnvironmentVar("eventStatus", event.getStatus());
		//above added by EP
		
		System.out.println("End of pause recording");
}
	
	private void stopEvent(Session session, lotus.domino.Document doc, PlatformClient client) 
	throws IOException, NumberFormatException, NotesException {
		System.out.println("Start of stop recording");
		
		CameraData cam = client.getCamera(doc.getItemValueInteger("gCameraId"));
		System.out.println(cam.getName() + " ****--- " + cam.getID() +   "--------*****"    + cam.getIdentifier()+ "*******"+ cam.getControlPort());
		EncoderClient enc = new EncoderClient(cam.getInternalIP(), cam.getControlPort() );
		
//		enc.loadMeeting(doc.getItemValueString("gMeetingId"));
		enc.stopMeeting();
		//below added by EP
		int eventID;
		eventID = doc.getItemValueInteger("gEventId");
		EventData event;
		event = client.getEvent(eventID);
		session.setEnvironmentVar("eventStatus", event.getStatus());
		//above added by EP
		System.out.println("End of stop recording");
	}

	
	private void tagEvent(lotus.domino.Document doc, PlatformClient client) 
	throws IOException, NumberFormatException, NotesException {
		boolean found = false;
		System.out.println("Start of tag recording");
		
		CameraData cam = client.getCamera(doc.getItemValueInteger("gCameraId"));
		System.out.println(cam.getName() + " ****--- " + cam.getID() +   "--------*****"    + cam.getIdentifier()+ "*******"+ cam.getControlPort());
		
		String intIP = cam.getInternalIP();
		System.out.println("cam.getInternalIP():" + intIP);
		int contPort = cam.getControlPort();
		System.out.println("cam.getControlPort():" + contPort);
		EncoderClient enc = new EncoderClient(intIP,contPort);
		System.out.println("after EncoderClient enc = new EncoderClient");
		//EncoderClient enc = new EncoderClient(cam.getInternalIP(), cam.getControlPort() );
		
//		enc.loadMeeting(doc.getItemValueString("gMeetingId"));		
		
		if (doc.getItemValueString("which").equalsIgnoreCase("Bills")) {		
			String billItem;
			Vector billsList = doc.getItemValue("allBills");
		     for (int i=0; i<billsList.size(); i++) {
		 		billItem = (String) billsList.elementAt(i);
		 		String[] bnum = billItem.split("\\|");
				System.out.println("billItem="+ billItem + " ----   bnum[0].trim()="+bnum[0].trim() + "    CBill="+doc.getItemValueString("CBill"));
				if ((bnum[0].trim()).equalsIgnoreCase(doc.getItemValueString("CBill"))) {
			 		String[] parts = bnum[1].split("~");	 		
					String billNum = parts[0].trim();
					String billTitle = parts[1].trim();
					String billSpons = parts[2].replace("^", ", ").trim();
					System.out.println("recorditem= " + billNum + ": " + billTitle + "\n (" +  billSpons + ")");
					enc.recordItem(billNum + ": " + billTitle + "\n (" +  billSpons + ")", "");
					found = true;
				}
			}
			if (!found) {
				System.out.println("in !found :" + doc.getItemValueString("CBill"));
				enc.recordItem(doc.getItemValueString("CBill"), "");				
			}

		} else {
			System.out.println("in else :" + doc.getItemValueString("CBill"));
			enc.recordItem(doc.getItemValueString("CBill"), "");	
		}
		System.out.println("End of tag recording");
	}
		
}

	     