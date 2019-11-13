/*-------------------------------------------------------------------------
*
*	Copyright (C) 2005, PostgreSQL Global Development Group
*
*--------------------------------------------------------------------------
*/
package org.postgresql.test.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.net.PGinet;
import java.sql.*;

/**
 *	Unit tests for the PGinet data type.
 *
 *	@author Russell Francis (russ@metro-six.com)
 */
public class PGinetTest extends BaseDatabaseTest
{
	private Connection dbConn;
	private static final String tableName = "testpginet";

	@Before
	public void setUp() throws Exception
	{
		dbConn = openDB();
		createTable( dbConn, this.tableName, "address inet" );
	}

	@After
	public void tearDown() throws Exception
	{
		dropTable( dbConn, this.tableName );
	}

	/**
 	 * 	This method will test that the PGinet type refuses to create
	 *	objects when passed invalid IPv4 based addresses.
	 */
	@Test
	public void testPGinetIPv4InvalidAddresses() 
	throws SQLException
	{
		String[] invalidAddresses = {
			"255.255.1/23",				// too short
			"255.255.128/16",			// too short
			"255.1/15",					// too short
			"255.128/8",				// too short
			"1/7",						// too short
			"128/0",					// too short
			"255.255.255.255/ab",		// non-numeric netmask
			"255.255.255.255/255/255",  // junk
			"300.0.0.1",				// a is out of range.
			"19.1000.50",				// b is out of range.
			"1.2.301.4/31",				// c is out of range.
			"1.2.3.4/35",				// netmask is too big.
			"1.2.3.4/-10",				// netmask is too small.
			"1.2.3.4/-1",				// netmask is too small.
			"1.2.3.4.5/32",				// not dotted quad.
			"a.b.c.d/32"				// hex digits not valid.
		};

		// Test that we can create networks 
		for (String invalidAddress : invalidAddresses) {
			PGinet address = this.makePGinet(invalidAddress);
			assertNull("An invalid address was turned into a PGinet object: " +
					invalidAddress + "' failed.", address);
		}
	}

	/**
 	 * 	This method will test that the PGinet type creates
	 *	PGinet objects when passed valid IPv4 based addresses.
 	 *	
	 *	<p>It will also ensure that they can be inserted into Postgres
	 *	and the they can be read from Postgres and that they maintain
	 *	equality after the ordeal.</p>
	 */
	@Test
	public void testPGinetIPv4ValidAddresses() throws SQLException
	{
		// Each address should be unique in the list or this test
		// will fail.
		String[] validAddresses = {
			"0.0.0.0",
			"192.168.1.10/32",
			"192.168.1.10/20",
			"10.10.10.120/8",
			"132.235.215.243",
			"200.46.204.71"
		};

		this.ensureValidAddresses( validAddresses );
	}

	/**
 	 * 	This method will test that the PGinet type refuses to create
	 *	objects when passed invalid IPv6 based addresses.
	 */
	@Test
	public void testPGinetIPv6InvalidAddresses()
	{
		String[] invalidAddresses = {
			"1234:1234:1234:1234:1234:1234:1234:1234:",			// superfluous trailing ':'
			":1234:1234:1234:1234:1234:1234:1234:1234",			// superfluous trailing ':'
			"::/-1",											// netmask out of range.
			"::/129",											// netmask out of range.
			"1234:5678:9abc:defg::/128",						// bogus hex value 'g'.
			"1234:5678:9abc::/0f",								// non numeric netmask.
			"1234:5678:9abc:deff:ffde:cba9:8765:4321:1234"		// to many hex sequences.
		};

		// Test that we cannot create the addresses
		for (String invalidAddress : invalidAddresses) {
			PGinet address = this.makePGinet(invalidAddress);
			assertNull("An invalid address was turned into a PGinet object: " +
					invalidAddress + "' failed.", address);
		}
	}

	/**
 	 * 	This method will test that the PGinet type creates
	 *	PGinet objects when passed valid IPv6 based address.
 	 *	
	 *	<p>It will also ensure that they can be inserted into Postgres
	 *	and the they can be read from Postgres and that they maintain
	 *	equality after the ordeal.</p>
	 */
	@Test
	public void testPGinetIPv6ValidAddresses() throws SQLException
	{
		// Each address should be unique in the list or this test
		// will fail.
		String[] validAddresses = {
			"::",
			"::/32",
			"abcd:eff0::/28",
			"abcd:efef::/32",
			"4bc:ab:1234::bcda/127",
			"4bc:ab:1234::bcda/128",
			"1234::1234",
			"1234:1234:1234:1234:1234:1234:1234:123E/127",
			"1234:1234:1234:1234:1234:1234:1234:123F/127",
			"1234:1234:1234:1234:1234:1234:1234:123F/128",
			"::192.168.1.1"
		};

		this.ensureValidAddresses( validAddresses );
	}

	/**
	 *	This method will take an array of valid address strings and
	 *	ensure that PGinet object can be created from them, these
	 *	objects are inserted into the database and each one is then fetched
	 *	from the database and their values are compared for equality.
	 *
	 *	<p>We also ensure that the hashCode for the original object
	 *	matches that of the fetched object.</p>
	 *
	 *	@param validAddresses an array of valid inet addresses.
	 */
	private void ensureValidAddresses( String[] validAddresses ) throws SQLException
	{
    // Insert each of the networks into the table.
    try (PreparedStatement s =
        dbConn.prepareStatement("INSERT INTO " + tableName + " (address) VALUES (?);")) {
      for (String value : validAddresses) {
        PGinet address = this.makePGinet(value);
        assertNotNull(
            "A valid address was unable to be converted into a PGinet object: '"
                + value
                + "' failed.",
            address);

        s.setObject(1, address);
        assertEquals(1, s.executeUpdate());
      }
		}

    try (PreparedStatement s =
        dbConn.prepareStatement("SELECT * FROM " + tableName + " WHERE address = ?")) {
      // Retrieve each of the networks from the table.
      for (String validAddress : validAddresses) {
        PGinet address = this.makePGinet(validAddress);
        assertNotNull(
            "A valid address was unable to be converted into a PGinet object: '"
                + validAddress
                + "' failed.",
            address);

        s.setObject(1, address);
				int count = 0;
        try (ResultSet rs = s.executeQuery()) {
          while (rs.next()) {
            PGinet fetchedAddress = (PGinet) rs.getObject(1);
            assertNotNull(
                "Unable to fetch inserted network from the database: '"
                    + validAddress
                    + "' failed.",
                address);

            assertEquals(
                "The retrieved address and the inserted address are not equal!",
                fetchedAddress,
                address);

            assertEquals(
                "The retrieved address and the inserted address have different hashCodes!",
                fetchedAddress.hashCode(),
                address.hashCode());
            ++count;
          }
				}
        assertEquals("Selected more than 1 row!", 1, count);
      }
		}
	}

	/**
	 *	This is a local utility method which will take a string
	 *	and attempt to create a new PGinet object.
	 *
	 *	<p>If the object cannot be created, null will be returned.</p>
	 *
	 *	@param value The textual representation of a network address.
	 *	@return null on failure or a PGinet object which represents
	 *		the value parameter.
	 */
	private PGinet makePGinet(String value)
	{
		try
		{
			return new PGinet(value);
		}
		catch(Exception e)
		{
			return null;
		}
	}
}
