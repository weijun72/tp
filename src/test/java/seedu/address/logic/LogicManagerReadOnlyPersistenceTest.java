package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.TypicalPersons.AMY;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Regression tests for read-only commands issued from a model with stale data.
 */
public class LogicManagerReadOnlyPersistenceTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_listFromStaleModel_doesNotOverwriteSavedStudent() throws Exception {
        Path dataPath = temporaryFolder.resolve("addressBook.json");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataPath);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        LogicManager staleLogic = new LogicManager(new ModelManager(), storage);

        AddressBook newerAddressBook = new AddressBook();
        newerAddressBook.addPerson(AMY);
        addressBookStorage.saveAddressBook(newerAddressBook);

        staleLogic.execute("list");

        assertEquals(List.of(AMY), addressBookStorage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_listWithoutDataFile_doesNotCreateOne() throws Exception {
        Path dataPath = temporaryFolder.resolve("addressBook.json");
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(dataPath),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));

        new LogicManager(new ModelManager(), storage).execute("list");

        assertFalse(Files.exists(dataPath));
    }
}
