package de.sysone.stellartest;

import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;


public class Main {

    public static final String FRIENDBOT_URL = "https://friendbot.stellar.org/?addr=%s";

    public static final String ACCOUNT1_PUBLIC = "GCIPE674LT2TKQVWCQM3IBLYWHEPZH3FP7KZ7JAOEID6FCR3WM4T67FZ";
    public static final String ACCOUNT1_SEEED = "SCI7OT562OL3BXP75CFAKBKRL6JABIG3TAZZMXSTQXR5OSYSJPHM7RFH";

    public static final String ACCOUNT2_PUBLIC = "GA3AOIY5ON7TQWOQSCKUHTMHT2USTQ4UGW6YKLFHLBFHUL7AUJDJXA4F";
    public static final String ACCOUNT2_SEEED = "SD6Z5NNN6WQ75H2VEUTLBPUW3HJ7E4GKPC4NCFI7AYUDEISIJCQITL5H";


    public static final String ACCOUNT3_PUBLIC = "GDRLJCGQ7UOOVXRHIUJN5KVWVLPAYGAUHQ5UJVWBTUOM7T6QA4X4TYHY";
    public static final String ACCOUNT3_SEEED = "SBXFSBJPU7U64DA33UHZT2XXP5ZBFIYIEUTI2BR6NGKBJVXE64AWUCNW";


    public static final String SOURCE_PUBLIC = "GGDPF2BRA6BBHWRR3E2GAHQQ6ROXW7IEHGOOVYE6BJ66AHGFIK6IYTNC5";
    public static final String SOURCE_SEEED = "SBDTIMFR6MEQFYSVBORXOEOPXHL32OFCCG2ORHDOV3OBA5Q4NUJK6JTJ";




    private static Server server;


    public static void main(String[] args) throws IOException {


        setupNetwork();

        //testTransaction();
        //testDoubleTransaction();

        KeyPair acc1 = KeyPair.fromSecretSeed(ACCOUNT1_SEEED);
        KeyPair acc2 = KeyPair.fromSecretSeed(ACCOUNT2_SEEED);
        KeyPair escrow = createEscrowAccount(acc1, acc2, "100");


        printAccountData(escrow);
        printAccountData(acc1);
        printAccountData(acc2);


        System.exit(0);

    }


    private static KeyPair createEscrowAccount(KeyPair acc1, KeyPair acc2, String amount) throws IOException {
        KeyPair escrow = createStellarEscrowAccount();

        printAccountData(acc1);
        printAccountData(acc2);

        fundEscrowAccount(acc1, acc2, amount, escrow);

        printAccountData(escrow);
        printAccountData(acc1);
        printAccountData(acc2);


        // create escrow returbn transaction: everyone gets their amount back, merge escrow back to soruce

        // create escrow dispute transaction: 10% go to the company fund, 90% to the community fund, escrow is then merged back to source


        return escrow;
    }

    private static void fundEscrowAccount(KeyPair acc1, KeyPair acc2, String amount, KeyPair escrow) throws IOException {
        System.out.println("Funding Escrow account");
        // todo - this should happen in one transaction, just in case 1 or 2 fails
        sendAmount(acc1, escrow, amount);
        sendAmount(acc2, escrow, amount);
    }

    private static KeyPair createStellarEscrowAccount() throws IOException {
        KeyPair escrow = KeyPair.random();
        System.out.println("Escrow Account:");
        System.out.println(escrow.getAccountId());
        System.out.println(new String(escrow.getSecretSeed()));

        System.out.println("looking up Source Account");
        KeyPair source = KeyPair.fromSecretSeed(SOURCE_SEEED);
        AccountResponse sourceAccount = server.accounts().account(source);
        printAccountData(source);

        // Create the escrow account and fund it
        Transaction createEscrowTransaction = new Transaction.Builder(sourceAccount)
                .addOperation(new CreateAccountOperation.Builder(escrow, "1").build())
                .build();
        createEscrowTransaction.sign(source);
        System.out.println("Submitting escrow account create transaction");
        submitTransaction(createEscrowTransaction);

        printAccountData(source);
        return escrow;
    }


    private static void testTransaction() throws IOException {
        KeyPair acc1 = KeyPair.fromSecretSeed(ACCOUNT1_SEEED);
        KeyPair acc2 = KeyPair.fromAccountId(ACCOUNT2_PUBLIC);


        printAccountData(acc1);
        printAccountData(acc2);

        sendAmount(acc1, acc2, "10");

        printAccountData(acc1);
        printAccountData(acc2);
    }

    private static void testDoubleTransaction() throws IOException {
        KeyPair acc1 = KeyPair.fromSecretSeed(ACCOUNT1_SEEED);
        KeyPair acc2 = KeyPair.fromAccountId(ACCOUNT2_PUBLIC);
        KeyPair acc3 = KeyPair.fromAccountId(ACCOUNT3_PUBLIC);


        printAccountData(acc1);
        printAccountData(acc2);
        printAccountData(acc3);

        send2twoAccounts(acc1, acc2, acc3, "22.222222", "33");

        printAccountData(acc1);
        printAccountData(acc2);
        printAccountData(acc3);
    }

    private static void send2twoAccounts(KeyPair acc1, KeyPair acc2, KeyPair acc3, String amount2, String amount3) throws IOException {
        AccountResponse sourceAccount = server.accounts().account(acc1);
        Transaction transaction = new Transaction.Builder(sourceAccount)
                .addOperation(new PaymentOperation.Builder(acc2, new AssetTypeNative(), amount2)
                        .build()).addOperation(new PaymentOperation.Builder(acc3, new AssetTypeNative(), amount3).build())
                .addMemo(Memo.text("Test double transaction"))
                .build();

        transaction.sign(acc1);

        System.out.println("submitting payment");


        submitTransaction(transaction);
    }

    private static SubmitTransactionResponse submitTransaction(Transaction transaction) {
        SubmitTransactionResponse response = null;
        try {
            response = server.submitTransaction(transaction);
            System.out.println("Success!");
            System.out.println(response);
        } catch (Exception e) {
            System.out.println("Something went wrong!");
            System.out.println(e.getMessage());

            // todo: failsafe retry a few times
            //response = server.submitTransaction(transaction);
        }
        return response;
    }

    private static void sendAmount(KeyPair acc1, KeyPair acc2, String amount) throws IOException {
        AccountResponse sourceAccount = server.accounts().account(acc1);
        Transaction transaction = new Transaction.Builder(sourceAccount)
                .addOperation(new PaymentOperation.Builder(acc2, new AssetTypeNative(), amount).build())
                .addMemo(Memo.text("Test Transaction"))
                .build();

        transaction.sign(acc1);

        System.out.println("submitting payment");

        submitTransaction(transaction);
    }

    private static void setupNetwork() {
        Network.useTestNetwork();
        server = new Server("https://horizon-testnet.stellar.org");
    }



    private static void getFriendBotFunding(KeyPair pair) throws IOException {
        String friendBotUrl;
        friendBotUrl = String.format(
                FRIENDBOT_URL,
                pair.getAccountId());

        InputStream response = new URL(friendBotUrl).openStream();
        String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
        System.out.println("created new funded account :)\n" + body);
    }

    private static void printAccountData(KeyPair pair) throws IOException {

        AccountResponse account = server.accounts().account(pair);
        System.out.println("* Balances for account " + pair.getAccountId());
        for (AccountResponse.Balance balance : account.getBalances()) {
            System.out.println(String.format(
                    " . Type: %s, Code: %s, Balance: %s",
                    balance.getAssetType(),
                    balance.getAssetCode(),
                    balance.getBalance()));
        }
    }

    public static KeyPair createFundedAccount() throws IOException {
        System.out.println("... creating new KeyPair...");
        KeyPair pair = KeyPair.random();
        System.out.println(pair.getAccountId());
        System.out.println(new String(pair.getSecretSeed()));
        getFriendBotFunding(pair);
        return pair;
    }

}
