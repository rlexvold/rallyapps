package com.sample.rally;

import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;

import com.rallydev.rest.RallyRestApi;

public class RallyOnPremApi extends RallyRestApi
{

	@SuppressWarnings("deprecation")
	public RallyOnPremApi(URI server, String userName, String password)
	{
		super(server, userName, password);

		try
		{
			SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy()
			{
				public boolean isTrusted(X509Certificate[] certificate, String authType) throws CertificateException
				{
					// trust all certs
					return true;
				}
			}, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, sf));
		}
		catch (Exception e)
		{
		}
	}
}