/*
  FILENAME - ErrorSim.java
  ASSIGNMENT - Final Project - SYSC 3303
  AUTHOR - GROUP 3 - W18
  DETAILS - A program that will receives DatagramPackets from Client or Server and forwards them to the opposite host.
*/

import java.net.*;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

class ErrorSim extends CommonMethods {
	private static final int CLIENT_PORT = 23;
	private static final int SERVER_PORT = 69;
	private static final int DATA_SIZE = 516;
	private static DatagramPacket rxPacket, txPacket;

	// testmode variables
	private static int mode = 0; // 0=normal,1=lose a packet,2=delay a packet,3=duplicate a packet and choose
									// when to send it, 4 = illegal packet , 5= unknown TID
	private static byte testOpcode = 0; // RRQ,WRQ,DATA,ACK,ERR
	private static int testBlockNum = -1; // 0 to 65335 - for lost/delayed/duplicate DATA/ACK
	private static long delay = 0; // sleep in milliseconds
	private static boolean isLost = false; // is current packet lost
	private static DatagramPacket testPacket = null; // used for Duplicate and Illegal packet test modes
	private static DatagramSocket testSocket; // used for Unknown TID
	private static int illegalField; // chosen field to invalidate: 1 = opcode, 2 = mode, 3 = errorCode, 4 = blockNum
										// .... May need to add more
	private static byte delayOpcode = 0; // DATA,ACK delayed packet opcode to replace
	private static int delayBkNum = 0; // block number to replace

	// get user input for test modes
	private static void getUserInput() {
		boolean inputValid = false;
		String input = "";

		boolean showMenu = true;
		boolean testMode = false;
		Scanner reader = new Scanner(System.in); // Reading from System.in

		while (showMenu) {
			inputValid = false;

			while (!inputValid) {
				System.out.print("Would you like to run the ErrorSim in (t)est or (n)ormal mode? ");
				input = reader.nextLine();
				input = input.toLowerCase();

				switch (input) {
				case "t":
					inputValid = true;
					showMenu = false;
					testMode = true; // testing operation
					break;
				case "n":
					inputValid = true;
					showMenu = false;
					testMode = false; // normal operation
					System.out.println("You've chosen normal operation.\n");
					break;
				default:
					System.out.println("The input is not valid.");
				}
			}
		}

		if (testMode) {
			System.out.print("\nTest Mode Options: \n" + "(1)\t- Lose a Packet\n" + "(2)\t- Delay a Packet\n"
					+ "(3)\t- Duplicate a Packet\n" + "(4)\t- Send an Illegal Packet\n"
					+ "(5)\t- Send from Unknown TID\n" + "(c)\t- Cancel Test Mode\n");

			inputValid = false;

			while (!inputValid) {
				System.out.print("Please choose an option: ");
				input = reader.nextLine();
				input = input.toLowerCase();

				switch (input) {
				case "1":
					inputValid = true;
					mode = 1;
					break;
				case "2":
					inputValid = true;
					mode = 2;
					break;
				case "3":
					inputValid = true;
					mode = 3;
					break;
				case "4":
					inputValid = true;
					mode = 4;
					break;
				case "5":
					inputValid = true;
					mode = 5;
					break;
				case "c":
					inputValid = true;
					testMode = false;
					System.out.println("You've chosen normal operation.\n");
					break;
				default:
					System.out.println("The input is not valid.");
				}
			}
		}

		if (mode != 0) {
			// 1=lose a packet,2=delay a packet,3=duplicate a packet and choose when to send
			// it
			inputValid = false;

			if (mode == 1) {
				System.out.print("\nLosing a Packet! \n");
				System.out.print("(1)\t- Read Request\n" + "(2)\t- Write Request\n" + "(3)\t- Data\n"
						+ "(4)\t- Acknowledgement\n" + "(5)\t- Error\n\n");

				while (!inputValid) {
					System.out.print("Which packet type is being dropped?  ");
					input = reader.nextLine();
					input = input.toLowerCase();

					switch (input) {
					case "1":
						inputValid = true;
						testOpcode = 1;
						System.out.println("The next RRQ will be dropped.\n");
						break;
					case "2":
						inputValid = true;
						testOpcode = 2;
						System.out.println("The next WRQ will be dropped.\n");
						break;
					case "3":
						inputValid = true;
						testOpcode = 3;
						break;
					case "4":
						inputValid = true;
						testOpcode = 4;
						break;
					case "5":
						inputValid = true;
						testOpcode = 5;
						System.out.println("The next ERROR will be dropped.\n");
						break;
					default:
						System.out.println("The input is not valid.");
					}
				}

				if (testOpcode == 3 || testOpcode == 4) {
					inputValid = false;

					while (!inputValid) {
						System.out.print("What packet/block number would you like to lose? ");
						input = reader.nextLine();
						int tempNum = -1;

						try {
							tempNum = Integer.valueOf(input);
							if (tempNum > 0) {
								inputValid = true;
								testBlockNum = tempNum;
							}
						} catch (Exception e) {
							System.out.println("The input is not valid.");
						}
					}
					if (testOpcode == 3)
						System.out.println("DATA Packet# " + testBlockNum + " will be dropped.\n");
					if (testOpcode == 4)
						System.out.println("ACK Packet# " + testBlockNum + " will be dropped.\n");

				}
			}

			if (mode == 2) {
				System.out.print("\nDelaying a Packet! \n");
				System.out.print("(1)\t- Read Request\n" + "(2)\t- Write Request\n" + "(3)\t- Data\n"
						+ "(4)\t- Acknowledgement\n" + "(5)\t- Error\n\n");

				while (!inputValid) {
					System.out.print("Which packet type is being delayed? ");
					input = reader.nextLine();
					input = input.toLowerCase();

					switch (input) {
					case "1":
						inputValid = true;
						testOpcode = 1;
						delayOpcode = 1;
						break;
					case "2":
						inputValid = true;
						testOpcode = 2;
						delayOpcode = 2;
						break;
					case "3":
						inputValid = true;
						testOpcode = 3;
						delayOpcode = 3;
						break;
					case "4":
						inputValid = true;
						testOpcode = 4;
						delayOpcode = 4;
						break;
					case "5":
						inputValid = true;
						testOpcode = 5;
						delayOpcode = 5;
						break;
					default:
						System.out.println("The input is not valid.");
					}
				}

				if (testOpcode == 3 || testOpcode == 4) {
					inputValid = false;

					while (!inputValid) {
						System.out.print("What packet/block number would you like to delay? ");
						input = reader.nextLine();
						int tempNum = -1;

						try {
							tempNum = Integer.valueOf(input);
							if (tempNum > 0) {
								inputValid = true;
								delayBkNum = tempNum;
							}
						} catch (Exception e) {
							System.out.println("The input is not valid.");
						}
					}
				}

				// Delay Amount
				inputValid = false;
				while (!inputValid) {
					System.out.print("How long will the packet be delayed (in ms)? ");
					input = reader.nextLine();
					int tempNum = -1;

					try {
						tempNum = Integer.valueOf(input);
						if (tempNum > 0) {
							inputValid = true;
							delay = tempNum;
						}
					} catch (Exception e) {
						System.out.println("The input is not valid.");
					}
				}

				if (testOpcode == 1)
					System.out.println("RRQ will be delayed by " + delay + " ms.\n");
				if (testOpcode == 2)
					System.out.println("WRQ will be delayed by " + delay + " ms.\n");
				if (testOpcode == 3)
					System.out.println("DATA Packet# " + delayBkNum + " will be delayed by " + delay + " ms.\n");
				if (testOpcode == 4)
					System.out.println("ACK Packet# " + delayBkNum + " will be delayed by " + delay + " ms.\n");
				if (testOpcode == 5)
					System.out.println("ERROR will be delayed by " + delay + " ms.\n");
			}

			if (mode == 3) {
				System.out.print("\nDuplicating a Packet! \n");
				System.out.print("(1)\t- Read Request\n" + "(2)\t- Write Request\n" + "(3)\t- Data\n"
						+ "(4)\t- Acknowledgement\n" + "(5)\t- Error\n\n");

				while (!inputValid) {
					System.out.print("Which packet type is being duplicated? ");
					input = reader.nextLine();
					input = input.toLowerCase();

					switch (input) {
					case "1":
						inputValid = true;
						testOpcode = 1;
						break;
					case "2":
						inputValid = true;
						testOpcode = 2;
						break;
					case "3":
						inputValid = true;
						testOpcode = 3;
						break;
					case "4":
						inputValid = true;
						testOpcode = 4;
						break;
					case "5":
						inputValid = true;
						testOpcode = 5;
						break;
					default:
						System.out.println("The input is not valid.");
					}
				}

				if (testOpcode == 3 || testOpcode == 4) {
					inputValid = false;

					while (!inputValid) {
						System.out.print("What packet/block number would you like to duplicate? ");
						input = reader.nextLine();
						int tempNum = -1;

						try {
							tempNum = Integer.valueOf(input);
							if (tempNum > 0) {
								inputValid = true;
								testBlockNum = tempNum;
							}
						} catch (Exception e) {
							System.out.println("The input is not valid.");
						}
					}
				}

				// Delay Amount
				inputValid = false;
				while (!inputValid) {
					System.out.print("How long will the packet be delayed (in ms)? ");
					input = reader.nextLine();
					int tempNum = -1;

					try {
						tempNum = Integer.valueOf(input);
						if (tempNum > 0) {
							inputValid = true;
							delay = tempNum;
						}
					} catch (Exception e) {
						System.out.println("The input is not valid.");
					}
				}

				if (testOpcode == 1)
					System.out.println("RRQ will be re-transmitted after " + delay + " ms.\n");
				if (testOpcode == 2)
					System.out.println("WRQ will be re-transmitted after " + delay + " ms.\n");
				if (testOpcode == 3)
					System.out.println(
							"DATA Packet# " + testBlockNum + " will be re-transmitted after " + delay + " ms.\n");
				if (testOpcode == 4)
					System.out.println(
							"ACK Packet# " + testBlockNum + " will be re-transmitted after " + delay + " ms.\n");
				if (testOpcode == 5)
					System.out.println("ERROR will be re-transmitted after " + delay + " ms.\n");
			}

			if (mode == 4) {// Illegal Packet - choose the packet and choose the field to invalidate
				// illegalField; //chosen field to invalidate: 1 = opcode, 2 = mode (only for
				// RRQ/WRQ), 3 = errorCode (only for errors), 4 = invalidate structure(replace
				// all zeroes) .... May need to add more
				System.out.print("\nInvalidating a Packet! \n");
				System.out.print("(1)\t- Read Request\n" + "(2)\t- Write Request\n" + "(3)\t- Data\n"
						+ "(4)\t- Acknowledgement\n" + "(5)\t- Error\n");

				while (!inputValid) {
					System.out.print("Which packet type is being invalidated?");
					input = reader.nextLine();
					input = input.toLowerCase();

					switch (input) {
					case "1":
						inputValid = true;
						testOpcode = 1;
						break;
					case "2":
						inputValid = true;
						testOpcode = 2;
						break;
					case "3":
						inputValid = true;
						testOpcode = 3;
						break;
					case "4":
						inputValid = true;
						testOpcode = 4;
						break;
					case "5":
						inputValid = true;
						testOpcode = 5;
						break;
					default:
						System.out.println("The input is not valid.");
					}
				}

				if (testOpcode == 3 || testOpcode == 4) {
					inputValid = false;

					while (!inputValid) {
						System.out.print("What packet/block number would you like to invalidate? ");
						input = reader.nextLine();
						int tempNum = -1;

						try {
							tempNum = Integer.valueOf(input);
							if (tempNum > 0) {
								inputValid = true;
								testBlockNum = tempNum;
							}
						} catch (Exception e) {
							System.out.println("The input is not valid.");
						}
					}
				}

				// illegalField; //chosen field to invalidate: 1 = opcode, 2 = mode (only for
				// RRQ/WRQ), 3 = errorCode (only for errors), 4 = invalidate structure(replace
				// all zeroes) .... May need to add more
				inputValid = false;
				while (!inputValid) {
					System.out.print("\nFields to Invalidate.\n");
					System.out.print("(1)\t- Op Code\n" + "(2)\t- Mode(Only for RRQ/WRQ)\n"
							+ "(3)\t- ErrorCode(Only for ERROR)\n" + "(4)\t- Structure\n");

					System.out.print("How would you like to invalidate the packet? ");
					input = reader.nextLine();

					// check that input is valid for the opcode
					if (input.equals("2") && (testOpcode != 1 && testOpcode != 2)) {
						input = "0";
						System.out.print("Simulator can only invalidate mode for RRQ/WRQ. ");
					} else if (input.equals("3") && testOpcode != 5) {
						input = "0";
						System.out.print("Simulator can only invalidate error code for ERROR. ");
					}

					switch (input) {
					case "1":
						inputValid = true;
						illegalField = 1;
						break;
					case "2":
						inputValid = true;
						illegalField = 2;
						break;
					case "3":
						inputValid = true;
						illegalField = 3;
						break;
					case "4":
						inputValid = true;
						illegalField = 4;
						break;
					default:
						System.out.println("The input is not valid.");
					}

				}

				if (testOpcode == 1)
					System.out.println("The next RRQ will be invalidated.\n");
				if (testOpcode == 2)
					System.out.println("The next WRQ will be invalidated.\n");
				if (testOpcode == 3)
					System.out.println("DATA Packet# " + testBlockNum + " will be invalidated.\n");
				if (testOpcode == 4)
					System.out.println("ACK Packet# " + testBlockNum + " will be invalidated.\n");
				if (testOpcode == 5)
					System.out.println("The next ERROR will be invalidated.\n");

			}

			if (mode == 5) {// Unknown TID - choose the packet to send
				System.out.print("\nPacket from an unknown TID!\n");
				System.out.print("(1)\t- Data\n" + "(2)\t- Acknowledgement\n" + "(3)\t- Error\n\n");

				while (!inputValid) {
					System.out.print("Which packet type is being sent from an unknown TID?  ");
					input = reader.nextLine();
					input = input.toLowerCase();

					switch (input) {
					case "1":
						inputValid = true;
						testOpcode = 3;
						break;
					case "2":
						inputValid = true;
						testOpcode = 4;
						break;
					case "3":
						inputValid = true;
						testOpcode = 5;
						System.out.println("The next ERROR will be sent from an unknown TID.\n");
						break;
					default:
						System.out.println("The input is not valid.");
					}
				}

				if (testOpcode == 3 || testOpcode == 4) {
					inputValid = false;

					while (!inputValid) {
						System.out.print("What packet/block number would you like to send? ");
						input = reader.nextLine();
						int tempNum = -1;

						try {
							tempNum = Integer.valueOf(input);
							if (tempNum > 0) {
								inputValid = true;
								testBlockNum = tempNum;
							}
						} catch (Exception e) {
							System.out.println("The input is not valid.");
						}
					}
					if (testOpcode == 3)
						System.out.println("DATA Packet# " + testBlockNum + " will be sent from an unknown TID.\n");
					if (testOpcode == 4)
						System.out.println("ACK Packet# " + testBlockNum + " will be sent from an unknown TID.\n");

				}
			}
		}
	}


	//A method that simulates errors for the server, takes as input the port of the sender thread and server socket
	public static boolean simulateServerError(int senderPort,DatagramSocket serverSocket ,boolean verboseOutput) throws Exception {

		byte[] rxData = rxPacket.getData();

		// --- Test Mode 1: Lost Packet ---//
		if (mode == 1 && rxData[1] == testOpcode) {
			if (rxData[1] == 1) {// if packet is RRQ
				isLost = true;
				System.out.println("** Dropped a RRQ! **\n");
			} else if (rxData[1] == 2) {// if packet is WRQ
				isLost = true;
				System.out.println("** Dropped a WRQ! **\n");
			} else if (rxData[1] == 5) {// if packet is ERROR
				isLost = true;
				System.out.println("** Dropped an ERROR! **\n");
			} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
					&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block number
				isLost = true;
				if (rxData[1] == 3)
					System.out.println("** Dropped a DATA (Block number " + testBlockNum + ") **\n");
				if (rxData[1] == 4)
					System.out.println("** Dropped an ACK (Block number " + testBlockNum + ") **\n");
			}
		}

		// --- Test Mode 2: Delay Packet ---//
		if (mode == 2 && rxData[1] == testOpcode) {
			if (rxData[1] == 1) {// If RRQ
				System.out.println("** Delayed RRQ by " + delay + "ms! **\n");
				Thread.sleep(delay);
				resetTestVars();
			} else if (rxData[1] == 2) {// If WRQ
				System.out.println("** Delayed WRQ by " + delay + "ms! **\n");
				Thread.sleep(delay);
				resetTestVars();
			} else if (rxData[1] == 5) {// if ERROR
				System.out.println("** Delayed ERROR by " + delay + "ms! **\n");
				Thread.sleep(delay);
				resetTestVars();
			} else if (blockNumToBytes(delayBkNum)[0] == rxData[2]
					&& blockNumToBytes(delayBkNum)[1] == rxData[3]) {// Otherwise check the block number
				if (rxData[1] == 3)
					System.out.println(
							"** Delayed DATA (Block number " + delayBkNum + ") by " + delay + "ms! **\n");
				if (rxData[1] == 4)
					System.out.println(
							"** Delayed ACK (Block number " + delayBkNum + ") by " + delay + "ms! **\n");

				Thread.sleep(delay);
				resetTestVars();
			}
		}

		// --- Test Mode 3: Duplicate Packet ---//
		Timer timer = new Timer();
		if (mode == 3 && rxData[1] == testOpcode) {
			if (rxData[1] == 1) {// If RRQ
				String msg = ("** Re-transmitted RRQ after " + delay + " ms!\n");
				timer.schedule(new resendPacket(txPacket, serverSocket, msg, direction.OUT, endhost.SERVER,
						verboseOutput), delay);
				resetTestVars();
			} else if (rxData[1] == 2) {// If WRQ
				String msg = ("** Re-transmitted WRQ after " + delay + " ms!\n");
				timer.schedule(new resendPacket(txPacket, serverSocket, msg, direction.OUT, endhost.SERVER,
						verboseOutput), delay);
				resetTestVars();
			} else if (rxData[1] == 5) {// if ERROR
				String msg = ("** Re-transmitted ERROR after " + delay + " ms!\n");
				timer.schedule(new resendPacket(txPacket, serverSocket, msg, direction.OUT, endhost.SERVER,
						verboseOutput), delay);
				resetTestVars();
			} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
					&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block number
				if (rxData[1] == 3) {
					String msg = ("** Re-transmitted DATA (Block number " + testBlockNum + ") after "
							+ delay + "ms! **\n");
					timer.schedule(new resendPacket(txPacket, serverSocket, msg, direction.OUT,
							endhost.SERVER, verboseOutput), delay);
					resetTestVars();
				}
				if (rxData[1] == 4) {
					String msg = ("** Re-transmitted ACK (Block number " + testBlockNum + ") after " + delay
							+ "ms! **\n");
					timer.schedule(new resendPacket(txPacket, serverSocket, msg, direction.OUT,
							endhost.SERVER, verboseOutput), delay);
					resetTestVars();
				}
			}
		}

		// --- Test Mode 4: Illegal Packet ---//
		if (mode == 4 && rxData[1] == testOpcode) {
			boolean isTestPacket = true;// Flag for checking testPacket opcode

			if (rxData[1] == 1) {// If RRQ
				System.out.println("** Replacing RRQ packet with illegal packet!\n");
			} else if (rxData[1] == 2) {// If WRQ
				System.out.println("** Replacing WRQ packet with illegal packet!\n");
			} else if (rxData[1] == 5) {// if ERROR
				System.out.println("** Replacing ERROR packet with illegal packet!\n");
			} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
					&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block number
				if (rxData[1] == 3) {
					System.out.println("** Replacing DATA (Block number " + testBlockNum
							+ ") with illegal packet! **\n");
				} else if (rxData[1] == 4) {
					System.out.println("** Replacing ACK (Block number " + testBlockNum
							+ ") with illegal packet! **\n");
				}
			} else {
				isTestPacket = false;
			}

			if (isTestPacket) {
				// illegalField; //chosen field to invalidate: 1 = opcode, 2 = mode, 3 =
				// errorCode, 4 = Replace all zeroes .... May need to add more
				if (illegalField == 1) {
					rxData[0] = 8;
					rxData[1] = 8;
				} else if (illegalField == 2) {// invalid mode should only be used for WRQ and RRQ
					int i = 2;// get position of mode
					while (rxData[i] != 0) {
						i++;
					}
					rxData[i + 1] = -1;// Only need to change one character of mode to invalidate it
				} else if (illegalField == 3) {
					rxData[2] = 9;
					rxData[3] = 9;
				} else if (illegalField == 4) {// replace all zeroes with ones
					for (int i = 0; i < rxData.length; i++) {
						if (rxData[i] == 0)
							rxData[i] = 1;
					}
				}
				txPacket.setData(rxData);
				resetTestVars();
			}
		}

		// --- Test Mode 5: Unknown TID ---//
		if (mode == 5 && rxData[1] == testOpcode) {
			boolean isTestPacket = true;// Flag for checking testPacket opcode

			if (rxData[1] == 1) {// If RRQ
				System.out.println("** Sending RRQ packet with unknown TID!\n");
			} else if (rxData[1] == 2) {// If WRQ
				System.out.println("** Sending WRQ packet with unknown TID!\n");
			} else if (rxData[1] == 5) {// if ERROR
				System.out.println("** Sending ERROR packet with unknown TID!\n");
			} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
					&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block number
				if (rxData[1] == 3) {
					System.out.println(
							"** Sending DATA (Block number " + testBlockNum + ") with unknown TID! **\n");
				} else if (rxData[1] == 4) {
					System.out.println(
							"** Sending ACK (Block number " + testBlockNum + ") with unknown TID! **\n");
				}
			} else {
				isTestPacket = false;
			}

			if (isTestPacket) {
				txPacket.setPort(senderPort);
				testSocket.send(txPacket);
				outputText(txPacket, direction.OUT, endhost.SERVER, verboseOutput);
				resetTestVars();
				return true;
			}
		}
		return false;
	}

	// Waits to receive packet from Client.java
	// Upon receipt, forwards it to Server.java
	// Waits to receive packet from Server.java
	// Upon receipt, forwards it to Client.java
	public static void main(String args[]) throws Exception {

		System.out.println("TFTP ErrorSim is running.\n");
		DatagramSocket clientSocket;
		DatagramSocket serverSocket;

		boolean verboseOutput = false;
		int tempPort = 0;
		int client_port = 0;
		InetAddress clientIP;

		clientSocket = new DatagramSocket(CLIENT_PORT);
		serverSocket = new DatagramSocket();
		testSocket = new DatagramSocket();

		byte[] rxData = new byte[DATA_SIZE];

		boolean lastDataPkt = false;
		getUserInput();

		while (true) {
			try {
				rxPacket = new DatagramPacket(rxData, rxData.length);

				// --- RECEIVE FROM CLIENT ---///
				clientSocket.receive(rxPacket);
				rxPacket = resizePacket(rxPacket);
				InetSocketAddress temp_add = (InetSocketAddress) rxPacket.getSocketAddress();
				clientIP=temp_add.getAddress();
				client_port = temp_add.getPort();

				if (isRequest(rxPacket))
					lastDataPkt = false;

				// ---START OF OUT OF BAND MANAGEMENT---//
				if (!isOOB(rxPacket)) {
					outputText(rxPacket, direction.IN, endhost.CLIENT, verboseOutput);
				} else {
					lastDataPkt = true;
					if (rxPacket.getData()[2] == 0)
						verboseOutput = false;
					else
						verboseOutput = true;
				}
				// ---END OF OUT OF BAND MANAGEMENT---//

				// Send to SERVER listener or last Thread
				txPacket = rxPacket;
				txPacket.setAddress(InetAddress.getLocalHost());
				if (rxData[1] == 1 || rxData[1] == 2 || tempPort == 0) {//send to server if RRQ/WRQ or if there is no previous sender
					txPacket.setPort(SERVER_PORT);
				} 	else {
					txPacket.setPort(tempPort);
				}

				if(mode != 0)//ERROR SIM to Server
					simulateServerError(tempPort,serverSocket,verboseOutput);

				if (!isLost) {// Don't send lost packet
					serverSocket.send(txPacket);
				}

				if (!isOOB(txPacket)) {
					if(!isLost)
						outputText(txPacket, direction.OUT, endhost.SERVER, verboseOutput);
				} else {
					lastDataPkt = true;
				}

				// --- RECEIVE FROM SERVER ---//
				if (!lastDataPkt && !isLost) {
					boolean needPkt = true;

					while (needPkt) {
						rxPacket = new DatagramPacket(rxData, rxData.length);
						serverSocket.receive(rxPacket);
						rxPacket = resizePacket(rxPacket);
						outputText(rxPacket, direction.IN, endhost.SERVER, verboseOutput);
						needPkt = false;

						if (rxPacket.getData().length < 512 && rxData[1] == 3)
							lastDataPkt = true;

						if (rxData[1] == 3 || rxData[1] == 4) {
							tempPort = rxPacket.getPort();
						}

						// --- Test Mode 1: Lost Packet ---//
						if (mode == 1 && rxData[1] == testOpcode) {
							if (rxData[1] == 5) {// if packet is ERROR
								needPkt = false;// Server request thread terminates after sending error
								System.out.println("** Dropped an ERROR! **\n");
								isLost = true;
								// resetTestVars();
							} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
									&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block
																						// number
								needPkt = true;
								if (rxData[1] == 3) {
									System.out.println("** Dropped a DATA (Block number " + testBlockNum + ") **\n");
								}
								if (rxData[1] == 4) {
									System.out.println("** Dropped an ACK (Block number " + testBlockNum + ") **\n");
								}
								resetTestVars();
							}
						}

						// --- Test Mode 2: Delay Packet ---//
						if (mode == 2 && rxData[1] == testOpcode) {
							if (rxData[1] == 1) {// If RRQ
								System.out.println("** Delayed RRQ by " + delay + "ms! **\n");
								Thread.sleep(delay);
								resetTestVars();
							} else if (rxData[1] == 2) {// If WRQ
								System.out.println("** Delayed WRQ by " + delay + "ms! **\n");
								Thread.sleep(delay);
								resetTestVars();
							} else if (rxData[1] == 5) {// if ERROR
								System.out.println("** Delayed ERROR by " + delay + "ms! **\n");
								Thread.sleep(delay);
								resetTestVars();
							} else if (blockNumToBytes(delayBkNum)[0] == rxData[2]
									&& blockNumToBytes(delayBkNum)[1] == rxData[3]) {// Otherwise check the block number
								if (rxData[1] == 3)
									System.out.println("** Delayed DATA (Block number " + delayBkNum + ") by " + delay
											+ "ms! **\n");
								if (rxData[1] == 4)
									System.out.println("** Delayed ACK (Block number " + delayBkNum + ") by " + delay
											+ "ms! **\n");

								Thread.sleep(delay);
								resetTestVars();
							}
						}

						// --- Test Mode 3: Duplicate Packet ---//
						Timer timer = new Timer();

						// Send to CLIENT
						txPacket = rxPacket;
						txPacket.setAddress(clientIP);
						txPacket.setPort(client_port);

						if (mode == 3 && rxData[1] == testOpcode) {
							if (rxData[1] == 1) {// If RRQ
								String msg = ("** Re-transmitted RRQ after " + delay + " ms!\n");
								timer.schedule(new resendPacket(txPacket, clientSocket, msg, direction.OUT,
										endhost.CLIENT, verboseOutput), delay);
								resetTestVars();
							} else if (rxData[1] == 2) {// If WRQ
								String msg = ("** Re-transmitted WRQ after " + delay + " ms!\n");
								timer.schedule(new resendPacket(txPacket, clientSocket, msg, direction.OUT,
										endhost.CLIENT, verboseOutput), delay);
								resetTestVars();
							} else if (rxData[1] == 5) {// if ERROR
								String msg = ("** Re-transmitted ERROR after " + delay + " ms!\n");
								timer.schedule(new resendPacket(txPacket, clientSocket, msg, direction.OUT,
										endhost.CLIENT, verboseOutput), delay);
								resetTestVars();
							} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
									&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block
																						// number
								if (rxData[1] == 3) {
									String msg = ("** Re-transmitted DATA (Block number " + testBlockNum + ") after "
											+ delay + "ms! **\n");
									timer.schedule(new resendPacket(txPacket, clientSocket, msg, direction.OUT,
											endhost.CLIENT, verboseOutput), delay);
									resetTestVars();
								}
								if (rxData[1] == 4) {
									String msg = ("** Re-transmitted ACK (Block number " + testBlockNum + ") after "
											+ delay + "ms! **\n");
									timer.schedule(new resendPacket(txPacket, clientSocket, msg, direction.OUT,
											endhost.CLIENT, verboseOutput), delay);
									resetTestVars();
								}
							}
						}

						// --- Test Mode 4: Illegal Packet ---//
						if (mode == 4 && rxData[1] == testOpcode) {
							boolean isTestPacket = true;// Flag for checking testPacket opcode

							if (rxData[1] == 1) {// If RRQ
								System.out.println("** Replacing RRQ packet with illegal packet!\n");
							} else if (rxData[1] == 2) {// If WRQ
								System.out.println("** Replacing WRQ packet with illegal packet!\n");
							} else if (rxData[1] == 5) {// if ERROR
								System.out.println("** Replacing ERROR packet with illegal packet!\n");
							} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
									&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block
																						// number
								if (rxData[1] == 3) {
									System.out.println("** Replacing DATA (Block number " + testBlockNum
											+ ") with illegal packet! **\n");
								} else if (rxData[1] == 4) {
									System.out.println("** Replacing ACK (Block number " + testBlockNum
											+ ") with illegal packet! **\n");
								}
							} else {
								isTestPacket = false;
							}

							if (isTestPacket) {
								// illegalField; //chosen field to invalidate: 1 = opcode, 2 = mode, 3 =
								// errorCode, 4 = Replace all zeroes .... May need to add more
								if (illegalField == 1) {
									rxData[0] = 9;
									rxData[1] = 9;
								} else if (illegalField == 2) {// invalid mode should only be used for WRQ and RRQ
									int i = 2;// get position of mode
									while (rxData[i] != 0) {
										i++;
									}
									rxData[i + 1] = -1;// Only need to change one character of mode to invalidate it
								} else if (illegalField == 3) {
									rxData[2] = 9;
									rxData[3] = 9;
								} else if (illegalField == 4) {// replace all zeroes with ones
									for (int i = 0; i < rxData.length; i++) {
										if (rxData[i] == 0)
											rxData[i] = 1;
									}
								}
								txPacket.setData(rxData);
								resetTestVars();
							}
						}

						// --- Test Mode 5: Unknown TID ---//
						if (mode == 5 && rxData[1] == testOpcode) {
							boolean isTestPacket = true;// Flag for checking testPacket opcode

							if (rxData[1] == 1) {// If RRQ
								System.out.println("** Sending RRQ packet with unknown TID!\n");
							} else if (rxData[1] == 2) {// If WRQ
								System.out.println("** Sending WRQ packet with unknown TID!\n");
							} else if (rxData[1] == 5) {// if ERROR
								System.out.println("** Sending ERROR packet with unknown TID!\n");
							} else if (blockNumToBytes(testBlockNum)[0] == rxData[2]
									&& blockNumToBytes(testBlockNum)[1] == rxData[3]) {// Otherwise check the block
																						// number
								if (rxData[1] == 3) {
									System.out.println("** Sending DATA (Block number " + testBlockNum
											+ ") with unknown TID! **\n");
								} else if (rxData[1] == 4) {
									System.out.println("** Sending ACK (Block number " + testBlockNum
											+ ") with unknown TID! **\n");
								}
							} else {
								isTestPacket = false;
							}

							if (isTestPacket) {
								testSocket.send(txPacket);
								outputText(txPacket, direction.OUT, endhost.CLIENT, verboseOutput);
								resetTestVars();
								if(rxData[1]==4)
									isLost=true;
								else//for Datas and errors
									needPkt = true;
							}
						}

					}

					if (!isLost) {// Don't send lost packet
						clientSocket.send(txPacket);
						outputText(txPacket, direction.OUT, endhost.CLIENT, verboseOutput);
					} else {
						resetTestVars();
					}
				} else {
					if (mode == 1)
						resetTestVars();
				}
			} catch (SocketTimeoutException ste) {
				System.out.println("Did not receive a packet from CLIENT or SERVER");
			}
		}

	}

	public static void resetTestVars() {
		// testmode variables
		mode = 0; // 0=normal,1=lose a packet,2=delay a packet,3=duplicate a packet and choose
					// when to send it
		testOpcode = 0; // RRQ,WRQ,DATA,ACK,ERR
		testBlockNum = -1; // 0 to 65335 - for lost/delayed/duplicate DATA/ACK
		delay = 0; // sleep in milliseconds
		isLost = false; // is current packet lost
		testPacket = null;
		delayOpcode = 0; // DATA,ACK delayed packet opcode to replace
		delayBkNum = 0; // block number to replace
	}
}

// Used in Test Mode 3: Duplicate Packet
// Re-sends the packet to the appropriate socket
class resendPacket extends TimerTask {

	DatagramPacket txPacket;
	DatagramSocket socket;
	String msg;
	CommonMethods.direction dir;
	CommonMethods.endhost host;
	boolean verbosOutput;

	resendPacket(DatagramPacket pkt, DatagramSocket socket, String msg, CommonMethods.direction dir,
			CommonMethods.endhost host, boolean verbose) {
		this.txPacket = pkt;
		this.socket = socket;
		this.msg = msg;
		this.dir = dir;
		this.host = host;
		this.verbosOutput = verbose;
	}

	public void run() {
		try {
			socket.send(txPacket);
			System.out.println(msg);
			CommonMethods.outputText(txPacket, dir, host, verbosOutput);
		} catch (Exception e) {
		}
	}

}
