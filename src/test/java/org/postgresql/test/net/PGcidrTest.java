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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.net.PGcidr;

/**
 *	Unit tests for the PGcidr data type.
 *
 *	@author Russell Francis (russ@metro-six.com)
 */
public class PGcidrTest extends BaseDatabaseTest
{
	private Connection dbConn;
	private static final String tableName = "testpgcidr";

	@Before
	public void setUp() throws SQLException {
		dbConn = openDB();
		createTable(dbConn, tableName, "network cidr");
	}

	@After
	public void tearDown() throws SQLException {
		dropTable(dbConn, tableName);
	}

	/**
 	 * 	This method will test that the PGcidr type refuses to create
	 *	objects when passed invalid IPv4 based network addresses.
	 */
	@Test
	public void testPGcidrIPv4InvalidNetworks() {
		String[] invalidNetworks = {
			"240.0.0.1/31",				// bits to the right of the netmask
			"255.255.1/23",				// bits to the right of the netmask
			"255.255.128/16",			// bits to the right of the netmask
			"255.1/15",					// bits to the right of the netmask
			"255.128/8",				// bits to the right of the netmask
			"1/7",						// bits to the right of the netmask
			"128/0",					// bits to the right of the netmask
			"255.255.255.255/ab",		// non-numeric netmask
			"255.255.255.255/255/255",  // junk
			"300.0.0.1",				// a is out of range.
			"19.1000.50",				// b is out of range.
			"1.2.301.4/31",				// c is out of range.
			"1.2.3.4/35",				// netmask is too big.
			"1.2.3.4/-10",				// netmask is too small.
			"1.2.3.4/-1",				// netmask is too small.
			"1.2.3.4.5/32"				// not dotted quad.
		};

		// Test that we can create networks 
		for (String invalidNetwork : invalidNetworks) {
			PGcidr network = this.makePGcidr(invalidNetwork);
			assertNull("An invalid network was turned into a PGcidr object: " +
					invalidNetwork + "' failed.", network);
		}
	}

	/**
 	 * 	This method will test that the PGcidr type creates
	 *	PGcidr objects when passed valid IPv4 based network addresses.
 	 *	
	 *	<p>It will also ensure that they can be inserted into Postgres
	 *	and the they can be read from Postgres and that they maintain
	 *	equality after the ordeal.</p>
	 */
	@Test
	public void testPGcidrIPv4ValidNetworks() throws SQLException
	{
		// Each network should be unique in the list or this test
		// will fail.
		String[] validNetworks = {
			"0",
			"0/32",
			"1",
			"1.5",
			"10.10",
			"10.10/15",
			"20.20.20",
			"20.20.20/25",
			"20.20.20/22",
			"220.200.200",
			"220.128.1/25",
			"220.200.200.4",
			"192.168.1.10/32"
		};

		this.ensureValidNetworks( validNetworks );
	}

	/**
 	 * 	This method will test that the PGcidr type refuses to create
	 *	objects when passed invalid IPv6 based network addresses.
	 */
	@Test
	public void testPGcidrIPv6InvalidNetworks()
	{
		String[] invalidNetworks = {
			"1234:1234:1234:1234:1234:1234:1234:1234:",			// superfluous trailing ':'
			"1234:1234:1234:1234:1234:1234:1234:123F/127",		// bits to right of netmask
			"1234:1234:1234::/31",								// bits to right of netmask
			"1234:1234:1234::/32",								// bits to right of netmask
			"1234:1234:1234::/34",								// bits to right of netmask
			"::/-1",											// netmask out of range.
			"::/129",											// netmask out of range.
			"1234:5678:9abc:defg::/128",						// bogus hex value 'g'.
			"1234:5678:9abc::/0f",								// non numeric netmask.
			"1234:5678:9abc:deff:ffde:cba9:8765:4321:1234"		// to many hex sequences.
		};

		// Test that we can create networks 
		for (String invalidNetwork : invalidNetworks) {
			PGcidr network = this.makePGcidr(invalidNetwork);
			assertNull("An invalid network was turned into a PGcidr object: " +
					invalidNetwork + "' failed.", network);

		}
	}


	/**
 	 * 	This method will test that the PGcidr type creates
	 *	PGcidr objects when passed valid IPv6 based network addresses.
 	 *	
	 *	<p>It will also ensure that they can be inserted into Postgres
	 *	and the they can be read from Postgres and that they maintain
	 *	equality after the ordeal.</p>
	 */
	@Test
	public void testPGcidrIPv6ValidNetworks() throws SQLException
	{
		// Each network should be unique in the list or this test
		// will fail.
		String[] validNetworks = {
			"::",
			"::/32",
			"abcd:eff0::/28",
			"abcd:efef::/32",
			"4bc:ab:1234::bcda/127",
			"4bc:ab:1234::bcda/128",
			"1234::1234",
			"1234:1234:1234:1234:1234:1234:1234:123E/127",
			"1234:1234:1234:1234:1234:1234:1234:123F/128"
		};

		this.ensureValidNetworks( validNetworks );
	}

	/**
	 *	This method will take an array of valid network strings and
	 *	ensure that PGcidr objects can be created from them, these
	 *	objects are inserted into the database and each one is then fetched
	 *	from the database and their values are compared for equality.
	 *
	 *	<p>We also ensure that the hashCode for the original object
	 *	matches that of the fetched object.</p>
	 *
	 *	@param validNetworks an array of valid network strings.
	 */
	private void ensureValidNetworks( String[] validNetworks )
	throws SQLException
	{
    // Insert each of the networks into the table.
    try (PreparedStatement s =
        dbConn.prepareStatement("INSERT INTO " + tableName + " (network) VALUES (?);")) {
			for (String validNetwork : validNetworks) {
				PGcidr network = this.makePGcidr(validNetwork);
				assertNotNull(
						"A valid network was unable to be converted into a PGcidr object: '"
								+ validNetwork
								+ "' failed.",
						network);

				s.setObject(1, network);
				assertEquals(1, s.executeUpdate());
			}
    }

    try (PreparedStatement s =
        dbConn.prepareStatement("SELECT network FROM " + tableName + " WHERE network = ?")) {
      // Retrieve each of the networks from the table.
			for (String validNetwork : validNetworks) {
				PGcidr network = this.makePGcidr(validNetwork);
				assertNotNull(
						"A valid network was unable to be converted into a PGcidr object: '"
								+ validNetwork
								+ "' failed.",
						network);

				s.setObject(1, network);
				int count = 0;
				try (ResultSet rs = s.executeQuery()) {
					while (rs.next()) {
						PGcidr fetchedNetwork = (PGcidr) rs.getObject(1);
						assertNotNull(
								"Unable to fetch inserted network from the database: '"
										+ validNetwork
										+ "' failed.",
								network);

						assertEquals(
								"The retrieved network and the inserted network are not equal!",
								fetchedNetwork,
								network);

						assertEquals(
								"The retrieved network and the inserted network have different hashCodes!",
								fetchedNetwork.hashCode(),
								network.hashCode());
						++count;
					}
				}
				assertEquals("Selected more than 1 row!", 1, count);
			}
		}
	}

	/**
	 *	This is a local utility method which will take a string
	 *	and attempt to create a new PGcidr object.
	 *
	 *	<p>If the object cannot be created, null will be returned.</p>
	 *
	 *	@param value The textual representation of a network address.
	 *	@return null on failure or a PGcidr object which represents
	 *		the value parameter.
	 */
	private PGcidr makePGcidr( String value )
	{
		try
		{
			return new PGcidr(value);
		}
		catch( Exception e )
		{
			return null;
		}
	}
}