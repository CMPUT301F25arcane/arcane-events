package com.example.arcane.repository;

import com.example.arcane.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserRepository
 */
@RunWith(MockitoJUnitRunner.class)
public class UserRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockCollectionRef;

    @Mock
    private DocumentReference mockDocumentRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    private UserRepository userRepository;
    private UserProfile testUser;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test user
        testUser = new UserProfile();
        testUser.setUserId("user123");
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setRole("ENTRANT");

        // Setup mock chain for Firestore
        when(mockDb.collection("users")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocumentRef);

        // Create UserRepository with mocked FirebaseFirestore (IoC)
        userRepository = new UserRepository(mockDb);
    }

    /**
     * Test createUser - verifies user is added to Firestore
     */
    @Test
    public void testCreateUser() {
        // Arrange
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDocumentRef.set(any(UserProfile.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = userRepository.createUser(testUser);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document("user123");
        verify(mockDocumentRef).set(testUser);
    }

    /**
     * Test getUserById - verifies user is retrieved by ID
     */
    @Test
    public void testGetUserById() {
        // Arrange
        String userId = "user123";
        Task<DocumentSnapshot> mockTask = Tasks.forResult(mockDocumentSnapshot);
        when(mockDocumentRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentSnapshot> result = userRepository.getUserById(userId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document snapshot", mockDocumentSnapshot, result.getResult());
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document(userId);
        verify(mockDocumentRef).get();
    }

    /**
     * Test updateUser - verifies user is updated in Firestore
     */
    @Test
    public void testUpdateUser() {
        // Arrange
        testUser.setName("Jane Doe Updated");
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDocumentRef.set(any(UserProfile.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = userRepository.updateUser(testUser);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document("user123");
        verify(mockDocumentRef).set(testUser);
    }

    /**
     * Test deleteUser - verifies user is deleted from Firestore
     */
    @Test
    public void testDeleteUser() {
        // Arrange
        String userId = "user123";
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDocumentRef.delete()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = userRepository.deleteUser(userId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document(userId);
        verify(mockDocumentRef).delete();
    }

    /**
     * Test getUserById with empty ID
     */
    @Test
    public void testGetUserByIdWithEmptyId() {
        // Arrange
        String emptyId = "";
        Task<DocumentSnapshot> mockTask = Tasks.forResult(mockDocumentSnapshot);
        when(mockDocumentRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method with empty ID
        Task<DocumentSnapshot> result = userRepository.getUserById(emptyId);

        // Assert - Repository should handle empty ID (Firestore allows it)
        assertNotNull("Result should not be null", result);
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document(emptyId);
    }

    /**
     * Test deleteUser with non-existent user ID
     */
    @Test
    public void testDeleteNonExistentUser() {
        // Arrange
        String userId = "nonexistent123";
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDocumentRef.delete()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = userRepository.deleteUser(userId);

        // Assert - Delete should still succeed even if document doesn't exist
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document(userId);
        verify(mockDocumentRef).delete();
    }

    /**
     * Test updateUser updates all fields
     */
    @Test
    public void testUpdateUserUpdatesAllFields() {
        // Arrange
        testUser.setName("Updated Name");
        testUser.setEmail("updated@example.com");
        testUser.setRole("ORGANIZER");

        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDocumentRef.set(any(UserProfile.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = userRepository.updateUser(testUser);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document("user123");
        verify(mockDocumentRef).set(testUser);

        // Verify the user object has updated values
        assertEquals("Name should be updated", "Updated Name", testUser.getName());
        assertEquals("Email should be updated", "updated@example.com", testUser.getEmail());
        assertEquals("Role should be updated", "ORGANIZER", testUser.getRole());
    }

    /**
     * Test createUser with complete UserProfile
     */
    @Test
    public void testCreateUserWithCompleteProfile() {
        // Arrange
        UserProfile completeUser = new UserProfile();
        completeUser.setUserId("user999");
        completeUser.setDeviceId("device999");
        completeUser.setName("Complete User");
        completeUser.setEmail("complete@example.com");
        completeUser.setRole("ADMIN");

        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDocumentRef.set(any(UserProfile.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = userRepository.createUser(completeUser);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("users");
        verify(mockCollectionRef).document("user999");
        verify(mockDocumentRef).set(completeUser);
    }
}