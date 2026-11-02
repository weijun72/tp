package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.TypicalPersons.AMY;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Checks that edits from one window do not discard newer changes from another window.
 */
public class LogicManagerConcurrentWriteTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_addFromStaleModel_rejectsChangeAndPreservesSavedStudent() throws Exception {
        Path dataPath = temporaryFolder.resolve("addressBook.json");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataPath);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        LogicManager staleLogic = new LogicManager(new ModelManager(), storage);

        AddressBook newerAddressBook = new AddressBook();
        newerAddressBook.addPerson(AMY);
        addressBookStorage.saveAddressBook(newerAddressBook);

        String addCommand = "add n/Charlie p/90000001 e/charlie@example.com a/Block One";
        CommandException exception = assertThrows(CommandException.class, () -> staleLogic.execute(addCommand));

        assertEquals("The data file changed in another app window. "
                + "Restart TutorTrack to load the latest students before making changes.", exception.getMessage());
        assertEquals(List.of(AMY), addressBookStorage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_successiveAddsInSameWindow_savesBothStudents() throws Exception {
        Path dataPath = temporaryFolder.resolve("addressBook.json");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataPath);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        LogicManager logic = new LogicManager(new ModelManager(), storage);

        logic.execute("add n/Charlie p/90000001 e/charlie@example.com a/Block One");
        logic.execute("add n/Dana p/90000002 e/dana@example.com a/Block Two");

        List<String> savedNames = addressBookStorage.readAddressBook().orElseThrow().getPersonList()
                .stream().map(Person::getName).map(name -> name.fullName).toList();
        assertEquals(List.of("Charlie", "Dana"), savedNames);
    }

    @Test
    public void execute_fileChangesAfterLoadBeforeLogicStarts_rejectsStaleChange() throws Exception {
        Path dataPath = temporaryFolder.resolve("addressBook.json");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataPath);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        AddressBook originalAddressBook = new AddressBook();
        originalAddressBook.addPerson(AMY);
        addressBookStorage.saveAddressBook(originalAddressBook);
        ReadOnlyAddressBook loadedAddressBook = storage.readAddressBook().orElseThrow();

        AddressBook newerAddressBook = new AddressBook(loadedAddressBook);
        newerAddressBook.addPerson(BOB);
        addressBookStorage.saveAddressBook(newerAddressBook);
        LogicManager staleLogic = new LogicManager(new ModelManager(loadedAddressBook, new UserPrefs()), storage);

        String addCommand = "add n/Charlie p/90000001 e/charlie@example.com a/Block One";
        assertThrows(CommandException.class, () -> staleLogic.execute(addCommand));

        assertEquals(List.of(AMY, BOB), addressBookStorage.readAddressBook().orElseThrow().getPersonList());
    }
}
