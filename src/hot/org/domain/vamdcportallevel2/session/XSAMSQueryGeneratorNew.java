package org.domain.vamdcportallevel2.session;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.domain.vamdcportallevel2.entity.ExtendedRegistry;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

@Scope(ScopeType.SESSION)
@Name("xsamsQueryNew")
public class XSAMSQueryGeneratorNew {

	@Logger
	private Log log;

	@In(create = true)
	private RegistryBrowser registryBrowser;

	private ArrayList<ExtendedRegistry> extendedRegistryList = null;
	private List<XSAMSQueryHeadResponse> headResponseList = null;
	private RegistryBrowserQueryThread registryBrowserQueryThread;
	private int threadsFinished = 0;

	private boolean pollEnabled = true;
	private Date startTime = null;

	private List<Future<XSAMSQueryHeadResponse>> futures;

	private ArrayList<AbstractQuery> submittedXSAMSQueryListTemp;

	@In(create = true)
	SpeciesAtoms speciesAtoms;

	@In(create = true)
	SpeciesMolecules speciesMolecules;

	@In(create = true)
	Transitions transitions;

	@In(create = true)
	Collisions collisions;

	@In(create = true)
	WavelengthWavelength wavelengthWavelength;

	@In(create = true)
	WavelengthWaveNumber wavelengthWaveNumber;

	@In(create = true)
	FreeForm freeForm;

	private QueryString query = null; // Query String encoded
	private String queryString;

	private String xsamsURL = null; // Populated by xsamInterface.page.xml which
									// receives xsam url
	private String queryResultFormat = "XSAMS";

	private List<String> selectedURLFromCheckBox;// = new ArrayList<String>();
	private List<String> selectedURLFromCheckBoxStage2;// = new
														// ArrayList<String>();
	// Default Value
	//private String speciesForm = "---";
	//private String wavelenthForm = "---";
	
	private boolean atomsForm = false;
	private boolean moleculesForm = false;
	private boolean transitionsForm = false;
	private boolean collisionsForm = false;

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	
	public boolean isAtomsForm() {
		return atomsForm;
	}

	public void setAtomsForm(boolean atomForm) {
		this.atomsForm = atomForm;
	}

	public boolean isMoleculesForm() {
		return moleculesForm;
	}

	public void setMoleculesForm(boolean moleculesForm) {
		this.moleculesForm = moleculesForm;
	}

	public boolean isTransitionsForm() {
		return transitionsForm;
	}

	public void setTransitionsForm(boolean transtionsForm) {
		this.transitionsForm = transtionsForm;
	}

	public boolean isCollisionsForm() {
		return collisionsForm;
	}

	public void setCollisionsForm(boolean collisionsForm) {
		this.collisionsForm = collisionsForm;
	}

	public boolean isPollEnabled() {
		return pollEnabled;
	}

	public void setPollEnabled(boolean pollEnabled) {
		this.pollEnabled = pollEnabled;
	}

	public List<String> getSelectedURLFromCheckBox() {
		return selectedURLFromCheckBox;
	}

	public ArrayList<AbstractQuery> getSubmittedXSAMSQueryListTemp() {
		return submittedXSAMSQueryListTemp;
	}

	public void setSubmittedXSAMSQueryListTemp(
			ArrayList<AbstractQuery> submittedXSAMSQueryListTemp) {
		this.submittedXSAMSQueryListTemp = submittedXSAMSQueryListTemp;
	}

	public void setSelectedURLFromCheckBox(
			List<String> selectedURLFromCheckBoxValue) {
		for (int i = 0; i < selectedURLFromCheckBoxValue.size(); i++) {
			System.out.println("setSelectedURLFromCheckBox: "
					+ selectedURLFromCheckBoxValue.get(i) + ": "
					+ selectedURLFromCheckBoxValue.size());
			// Due to table each selected check box is sent separately
			// The last check box overwrite previous values
			// Created another check box to overcome this issue.
			this.selectedURLFromCheckBoxStage2.add(selectedURLFromCheckBoxValue
					.get(i));
		}
		this.selectedURLFromCheckBox = selectedURLFromCheckBoxValue;
	}

	private boolean registryBrowserQuery = true;

	public void populateExtendedRegistryList() {

		if (registryBrowserQuery == true) {
			registryBrowserQueryThread = new RegistryBrowserQueryThread(
					registryBrowser);

			Thread thread = new Thread(registryBrowserQueryThread);

			thread.start();
			registryBrowserQuery = false;
		}
	}

	public void executeQueryStage2() {
		// IndexInList is index of submitted queries
		int indexInList = -99;
		XSAMSQueryAdaptor xsamsAdaptorTemp = null;
		Thread tempXSAMSThread = null;
		ExtendedRegistry tempExtendedRegistry = null;
		submittedXSAMSQueryListTemp = new ArrayList<AbstractQuery>();

		for (int i = 0; i < this.selectedURLFromCheckBoxStage2.size(); i++) {
			System.out.println("executeQueryStage2: "
					+ this.selectedURLFromCheckBoxStage2.get(i) + "  : "
					+ selectedURLFromCheckBoxStage2.size());

			tempExtendedRegistry = registryBrowser
					.getExtendedResource(selectedURLFromCheckBoxStage2.get(i));

			xsamsAdaptorTemp = new XSAMSQueryAdaptor(indexInList,
					selectedURLFromCheckBoxStage2.get(i) + "sync?", query,
					queryString);

			xsamsAdaptorTemp.setExtendedRegistry(tempExtendedRegistry);

			xsamsAdaptorTemp.setQueryType(queryResultFormat);
			registryBrowser.getSubmittedTAPQueryList().add(0, xsamsAdaptorTemp);
			submittedXSAMSQueryListTemp.add(0, xsamsAdaptorTemp);

			tempXSAMSThread = new Thread(xsamsAdaptorTemp);
			tempXSAMSThread.start();

		}
		selectedURLFromCheckBox = selectedURLFromCheckBoxStage2;
		clearForm();
	}

	public void executeQueryStage1() {
		log.info("XSAMSQueryNew.executeQuery() action called:  "
				+ this.atomsForm + " " + this.moleculesForm + " " + this.transitionsForm + " " + this.collisionsForm);

		queryString = "SELECT ALL WHERE ";
		if (atomsForm == true) {
			queryString = queryString + speciesAtoms.getQueryString() + " ";
		} 
		
		if (moleculesForm == true) {
			if (atomsForm == true) {
				queryString = queryString + " AND ";
			}
			queryString = queryString + speciesMolecules.getQueryString();
		}

		if (transitionsForm == true) {
			if (atomsForm == true || moleculesForm == true){
				queryString = queryString + " AND ";
			}
			queryString = queryString + transitions.getQueryString();
		}

		if (collisionsForm == true) {
			if (atomsForm == true || moleculesForm == true || transitionsForm == true) {
				queryString = queryString + " AND ";
			}
			queryString = queryString + collisions.getQueryString();
		}
		submitHeadRequest();
	}

	public void executeQueryStage1FreeForm() {
		queryString = "";
		queryString = freeForm.getQueryString();
		submitHeadRequest();
	}

	private void submitHeadRequest() {

		this.query = new QueryString("REQUEST", "doQuery");
		this.query.add("LANG", "VSS1");
		this.query.add("FORMAT", queryResultFormat);
		this.query.add("QUERY", queryString);

		log.info("XSAMSQuery: " + query.toString());

		System.out.println(queryString);

		if (extendedRegistryList == null) {
			extendedRegistryList = registryBrowserQueryThread
					.getExtendedRegistryList();
		}

		futures = new ArrayList<Future<XSAMSQueryHeadResponse>>(
				extendedRegistryList.size());

		for (int i = 0; i < extendedRegistryList.size(); i++) {
			System.out.println(extendedRegistryList.get(i).getResource()
					.getTitle()
					+ "  " + extendedRegistryList.get(i).getXsamURL());

			final ExecutorService service = Executors.newFixedThreadPool(15);
			/*
			 * if (!extendedRegistryList.get(i).getResource().getTitle()
			 * .contains("BASECOL")) {
			 */
			System.out.println(extendedRegistryList.get(i).getResource()
					.getTitle()
					+ "  " + extendedRegistryList.get(i).getXsamURL());
			futures.add(service.submit(new XSAMSQueryHeadRequestThread(
					extendedRegistryList.get(i).getResource().getTitle(),
					extendedRegistryList.get(i).getXsamURL(), query.toString())));
			startTime = new Date();
			// }
		}

		pollEnabled = true;
		headResponseList = new ArrayList<XSAMSQueryHeadResponse>(
				extendedRegistryList.size());
		selectedURLFromCheckBox = new ArrayList<String>();
		selectedURLFromCheckBoxStage2 = new ArrayList<String>();

		System.out.println("headResponseList.isEmpty(): "
				+ headResponseList.isEmpty());
		// Thread.sleep(1000)
		new Thread() {
			public void run() {
				if (futures != null) {
					int futuresLength = futures.size();
					int counter = 1;
					threadsFinished = 0;
					for (Future<XSAMSQueryHeadResponse> future : futures) {
						System.out.println(future.isDone() + " "
								+ (new Date().getTime() - startTime.getTime()));
						if (!future.isDone()
								&& (new Date().getTime() - startTime.getTime()) > 20000) {
							System.out.println("in If condition " + future.isDone() + " "
									+ (new Date().getTime() - startTime.getTime()));
							counter++;
							future.cancel(true);
						} else {
							System.out.println("Else Before sleep: " + future.isDone() + " "
									+ (new Date().getTime() - startTime.getTime()));
							if (counter++ == futuresLength) {
								new Thread(new HeadResponseParserThread(future,
										true)).start();
							} else {
								new Thread(new HeadResponseParserThread(future,
										false)).start();
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out.println("Else After sleep: " + future.isDone() + " "
									+ (new Date().getTime() - startTime.getTime()));
						}
					}
					// try { Thread.sleep(1000); } catch (Exception e){}
					// pollEnabled = false;
					System.out.println("headResponseList.isEmpty(): "
							+ headResponseList.isEmpty());
				}
			}
		}.start();
	}

	public List<XSAMSQueryHeadResponse> getHeadResponseList() {
		// System.out.println("Query Polling");
		return headResponseList;
	}

	/* */
	class HeadResponseParserThread implements Runnable {
		Future<XSAMSQueryHeadResponse> future;
		boolean lastThread = false;

		HeadResponseParserThread(Future<XSAMSQueryHeadResponse> futureValue,
				boolean lastThreadValue) {
			this.future = futureValue;
			this.lastThread = lastThreadValue;
		}

		public void run() {
			try {
				if (future != null) {
				}
				XSAMSQueryHeadResponse tempXSAMSQueryHeadResponse = future
						.get();
				synchronized (this) {
					headResponseList.add(tempXSAMSQueryHeadResponse);
				}
				// try { Thread.sleep(500); } catch (Exception e){}
				System.out.println("Respnse Received for: " + queryString);
				if (tempXSAMSQueryHeadResponse.getStatusCode().equals("200")) {
					selectedURLFromCheckBox.add(tempXSAMSQueryHeadResponse
							.getURL());
				}
				threadsFinished = threadsFinished + 1;
				if (threadsFinished == futures.size()) {
					pollEnabled = false;
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void clearForm() {
		speciesAtoms.clearFields();
		speciesMolecules.clearFields();
		transitions.clearFields();
		collisions.clearFields();
		//wavelengthWavelength.clearFields();
		//wavelengthWaveNumber.clearFields();
		freeForm.setQueryString("SELECT ALL WHERE ");
	}
	
	public void toggleEditable(){
		speciesAtoms.toggleEditable();
		speciesMolecules.toggleEditable();
		transitions.toggleEditable();
		collisions.toggleEditable();
	}

}
