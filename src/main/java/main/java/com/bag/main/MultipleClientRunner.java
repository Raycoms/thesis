package main.java.com.bag.main;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Runs multiple clients at the same time.
 */
public class MultipleClientRunner
{

    public class OutputPrinter extends Thread
    {
        private InputStream stream;
        private String      clientName;

        public OutputPrinter(String clientName, InputStream stream)
        {
            this.stream = stream;
            this.clientName = clientName;
        }

        @Override
        public void run()
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null)
                {
                    System.out.println(clientName + ": " + line);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Standard constructor.
     */
    public MultipleClientRunner()
    {
        /*
         * Intentionally lef empty.
         */
    }

    /**
     * Run defined number of clients.
     * @param option (if bag or direct)
     * @param processId id of the process, shold be unique for each process starting with zero
     * @param numOfClients number of clients to start (the ids will be generated according to processId,
     *                     (ex.: processId=0,numOfClients=3 will span clients 0, 1, 2
     *                           processId=1,numOfClients=3 will span clients 3, 4, 5)
     * @param percOfWrites percentage of writes
     * @param numOfServers amount of servers to contact. If <= 0 will only this server will be used (after an abs)
     * @param localClusterId id of accessed local cluster.
     * @param addresses addresses of servers to contact.
     */
    private void runClients(final String option, final int processId, final int numOfClients, final double percOfWrites,
                            final int numOfServers, final int localClusterId, final String addresses)
    {
        try
        {
            Random rnd = new Random();
            System.out.printf("Starting...\n");
            List<Process> procs = new ArrayList<Process>();
            String[] directAddresses = new String[0];
            if (addresses != null)
            {
                directAddresses = addresses.split(",");
            }

            int clientId = Integer.parseInt(Integer.toString(processId) + Integer.toString(numOfClients));

            for (int i = 0; i < numOfClients; i++)
            {
                int serverPartner = numOfServers <= 0 ? Math.abs(numOfServers) : clientId % numOfServers;
                String cmd;
                if (option.equals("bag"))
                {
                    cmd = String.format("java -cp build/libs/1.0-0.1-Setup-fat.jar main.java.com.bag.main.RunTests true %d %d %d %s",
                            serverPartner, localClusterId, clientId, String.valueOf(percOfWrites).replace(',', '.'));
                }
                else if (option.equals("direct"))
                {
                    serverPartner = i % directAddresses.length;
                    String[] address = directAddresses[serverPartner].split(":");
                    cmd = String.format("java -cp build/libs/1.0-0.1-Setup-fat.jar main.java.com.bag.main.RunTests false %s %s %d %s",
                            address[0], address[1], clientId, String.valueOf(percOfWrites).replace(',', '.'));
                }
                else
                {
                    System.out.println("Invalid option " + option);
                    return;
                }
                System.out.printf("Running Command: %s\n", cmd);
                /*ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-e", cmd);
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process proc = pb.start();*/
                Process proc = Runtime.getRuntime().exec(cmd);

                OutputPrinter printer = new MultipleClientRunner.OutputPrinter("Client " + clientId, proc.getInputStream());
                printer.start();
                procs.add(proc);
                clientId += 1;
                Thread.sleep(rnd.nextInt(200));
            }

            for (Process p : procs)
            {
                p.waitFor();
            }

            System.out.printf("Finishing...\n");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        if (args.length < 5)
        {
            System.out.println("Usage - BAG: MultipleClientRunner bag processId numOfClientsToRun percOfWrites numOfServers localClusterId");
            System.out.println("Usage - Direct: MultipleClientRunner bag processId numOfClientsToRun percOfWrites numOfServers addressesSeparatedByCommas");
            System.out.println("Each process should have an unique processId. The ids of the clients will be generated according to the processId");
        }

        String opt = args[0];
        String address = null;
        int processId = Integer.parseInt(args[1]);
        int numOfClients = Integer.parseInt(args[2]);
        double percOfWrites = Double.parseDouble(args[3]);
        int numOfServers = Integer.parseInt(args[4]);
        int localClusterId = Integer.parseInt(args[5]);
        if (args.length > 6)
        {
            address = args[6];
        }

        MultipleClientRunner runner = new MultipleClientRunner();
        runner.runClients(opt, processId, numOfClients, percOfWrites, numOfServers, localClusterId, address);
    }
}
