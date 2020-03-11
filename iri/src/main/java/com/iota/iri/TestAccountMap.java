import com.iota.iri.utils.Converter;
import com.iota.iri.controllers.TransactionViewModel;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.DatatypeConverter;
import javax.xml.crypto.Data;

import com.google.common.base.Strings;

import com.iota.iri.vm.datasource.rocksdb.RocksDbDataSource;
import com.iota.iri.vm.datasource.JournalSource;
import com.iota.iri.vm.Repository;
import com.iota.iri.vm.RepositoryRoot;
import com.iota.iri.vm.datasource.DbSettings;
import com.iota.iri.vm.datasource.DbSource;
import com.iota.iri.vm.vm.DataWord;
import com.iota.iri.TransactionExecutor;
import com.iota.iri.vm.EVMTransaction;
import com.iota.iri.vm.program.invoke.ProgramInvokeFactory;
import com.iota.iri.vm.program.invoke.ProgramInvokeFactoryImpl;
import com.iota.iri.vm.config.CommonConfig;

// import com.iota.iri.vm.datasource.leveldb.LevelDbDataSource;
// import com.iota.iri.vm.datasource.DbSource;
// import org.iota.jota.module.Transfer;

public class TestAccountMap{

    public static void main(String[] args) {
        byte[] root = createDB();
        getDB(root);
        // createDB();

        // TransactionExecutionSummary summary = executor.finalization();
    }

    private static void getDB(byte[] root){
        CommonConfig commonConfig = new CommonConfig();
        DbSource<byte[]> stateDb = new RocksDbDataSource("test");
        stateDb.init(DbSettings.DEFAULT);
        JournalSource<byte[]> pruningStateDS = new JournalSource<>(stateDb);
        RepositoryRoot track = new RepositoryRoot(pruningStateDS, root);
        String IOTAAddr = "ZLGVEQ9JUZZWCZXLWVNTHBDX9G9KZTJP9VEERIIFHY9SIQKYBVAHIMLHXPQVE9IXFDDXNHQINXJDRPFDXNYVAPLZAW";
        System.out.println("============================================");

        System.out.println("AccountStateExists?"+(track.getAccountState(Converter.trytesToBytes(IOTAAddr)) != null));

        System.out.println("AccountMapExists?"+(track.getAccountMap(Converter.trytesToBytes(IOTAAddr)) != null));


        System.out.println("============================================");

    }

    private static byte[] createDB() {
        ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
        CommonConfig commonConfig = new CommonConfig();

        DbSource<byte[]> stateDb = new RocksDbDataSource("test");
        stateDb.init(DbSettings.DEFAULT);
        JournalSource<byte[]> pruningStateDS = new JournalSource<>(stateDb);
        RepositoryRoot track = new RepositoryRoot(pruningStateDS);
        String IOTAAddr = "ZLGVEQ9JUZZWCZXLWVNTHBDX9G9KZTJP9VEERIIFHY9SIQKYBVAHIMLHXPQVE9IXFDDXNHQINXJDRPFDXNYVAPLZAW";
        byte[] fromAddr = DatatypeConverter.parseHexBinary("0000000000000000000000000000000000000000");
        track.aliasAccount(Converter.trytesToBytes(IOTAAddr),fromAddr, null);
        track.createAccount(Converter.trytesToBytes(IOTAAddr));
        System.out.println("============================================");

        System.out.println("AccountStateExists?"+(track.getAccountState(Converter.trytesToBytes(IOTAAddr)) != null));

        // System.out.println("AccountStateExists?"+track.getAccountState(Converter.trytesToBytes(IOTAAddr)).getNonce());


        System.out.println("AccountMapExists?"+(track.getAccountMap(Converter.trytesToBytes(IOTAAddr)) != null));
        // System.out.println("AccountMapExists?"+DatatypeConverter.printHexBinary(track.getAccountMap(Converter.trytesToBytes(IOTAAddr)).getEVMAddr()));

        System.out.println("============================================");
        track.flush();
        byte[] root = track.getRoot();
        stateDb.close();
        return root;

    }
}