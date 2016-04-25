// requried imports
import java.text.DateFormat
import java.text.SimpleDateFormat
import wslite.soap.SOAPClient
import wslite.soap.SOAPResponse

// setup SOAP client
def url = " https://customdev.journaltech.com/api/soap/TicketAPI.svc";
def SOAPClient soapClient = new SOAPClient(url);
def DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
def dateCreated = new Date();
def dateExpires = new Date(System.currentTimeMillis() + 180000) // adding 3 mintues
def newID = UUID.randomUUID();
def timeStampID = "Timestamp-${newID}";
def tokenID = "Usertoken-${newID}";

// check for new tickets
SOAPResponse soapResponse = soapClient.send(SOAPAction: "http://tempuri.org/ITicketAPI/getNewTickets") {
  	soapNamespacePrefix "env"
  	envelopeAttributes "xmlns:wsse" : "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "xmlns:wsu" : "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "xmlns:api" : "http://tempuri.org/"
  	header {
      	"wsse:Security"("env:mustUnderstand" : "1") {
          	"wsu:Timestamp"("wsu:Id" : timeStampID) {
          		"wsu:Created"(dateFormat.format(dateCreated))
              	"wsu:Expires"(dateFormat.format(dateExpires))
            }
          	"wsse:UsernameToken"("wsu:Id" : tokenID) {
          		"wsse:Username"("USERNAME")
              	"wsse:Password"("Type" : "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText", "PASSWORD")
          	}
      	}
    }
    body {
      getNewTickets(xmlns:'http://tempuri.org/')
  	}
};


// create a new person record
Person newPerson = new Person();
newPerson.firstName = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].FirstName[0];
newPerson.middleName = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].MiddleName[0];
newPerson.lastName = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].LastName[0];
newPerson.saveOrUpdate();

// add the drivers license number to the person record
Identification dlNumber = new Identification();
dlNumber.identificationClass = "STID";  // STID: State Identification
dlNumber.identificationType = "DL";     // DL:   Driver's License Number
dlNumber.identificationNumber = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].DriversLicenseNumber[0];
dlNumber.associatedPerson = newPerson;
dlNumber.saveOrUpdate();

// create the traffic newTicket case
Case newCase = new Case();
newCase.caseNumber = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].TicketID[0];
newCase.caseType = "TRAFFIC";
newCase.saveOrUpdate();

// add person to the case as a newParty
Party newParty = new Party();
newParty.case = newCase;
newParty.partyType = 'DEF'; // DEF: Defendant
newParty.person = newPerson;
newParty.saveOrUpdate();

// add the newCharge
Charge newCharge = new Charge();
newCharge.associatedParty = newParty;
newCharge.chargeType = "TRAF";  // TRAF: Traffic Citation
newCharge.description = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].ViolationDesc[0];
newCharge.chargeNumber = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].StatuteCode[0];
newCharge.location = soapResponse.getNewTicketsResponse.getNewTicketsResult.Ticket[0].Location[0];
newCharge.saveOrUpdate();


// done!


