/*
 * Copyright 2011-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.Threading;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import de.schildbach.wallet.Constants;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.util.BitmapFragment;
import de.schildbach.wallet.util.Nfc;
import de.schildbach.wallet.util.Qr;
import de.schildbach.wallet.util.ThrottlingWalletChangeListener;
import de.schildbach.wallet.util.WalletUtils;
import de.schildbach.wallet_test.R;

/**
 * @author Andreas Schildbach
 */
public final class WalletAddressFragment extends Fragment
{
	private Activity activity;
	private WalletApplication application;
	private Wallet wallet;
	private NfcManager nfcManager;
	private LocalBroadcastManager broadcastManager;

	private ImageView bitcoinAddressQrView;

	private Address lastSelectedAddress;

	private Bitmap qrCodeBitmap;
	private Spanned qrCodeLabel;

	@Override
	public void onAttach(final Activity activity)
	{
		super.onAttach(activity);

		this.activity = activity;
		this.application = (WalletApplication) activity.getApplication();
		this.wallet = application.getWallet();
		this.nfcManager = (NfcManager) activity.getSystemService(Context.NFC_SERVICE);
		this.broadcastManager = LocalBroadcastManager.getInstance(activity.getApplicationContext());
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		final View view = inflater.inflate(R.layout.wallet_address_fragment, container, false);
		bitcoinAddressQrView = (ImageView) view.findViewById(R.id.bitcoin_address_qr);

		bitcoinAddressQrView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				handleShowQRCode();
			}
		});

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		wallet.addEventListener(walletChangeListener, Threading.SAME_THREAD);
		broadcastManager.registerReceiver(walletChangeReceiver, new IntentFilter(WalletApplication.ACTION_WALLET_CHANGED));

		updateView();
	}

	@Override
	public void onPause()
	{
		broadcastManager.unregisterReceiver(walletChangeReceiver);
		wallet.removeEventListener(walletChangeListener);

		Nfc.unpublish(nfcManager, activity);

		super.onPause();
	}

	private void updateView()
	{
		final Address address = wallet.currentReceiveAddress();

		if (!address.equals(lastSelectedAddress))
		{
			lastSelectedAddress = address;

			final String addressStr = BitcoinURI.convertToBitcoinURI(address, null, null, null);

			final int size = (int) (256 * getResources().getDisplayMetrics().density);
			qrCodeBitmap = Qr.bitmap(addressStr, size);
			bitcoinAddressQrView.setImageBitmap(qrCodeBitmap);

			qrCodeLabel = WalletUtils.formatAddress(address, Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE);

			Nfc.publishUri(nfcManager, activity, addressStr);
		}
	}

	private void handleShowQRCode()
	{
		BitmapFragment.show(getFragmentManager(), qrCodeBitmap, qrCodeLabel);
	}

	private final BroadcastReceiver walletChangeReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			updateView();
		}
	};

	private final ThrottlingWalletChangeListener walletChangeListener = new ThrottlingWalletChangeListener()
	{
		@Override
		public void onThrottledWalletChanged()
		{
			updateView();
		}
	};
}
