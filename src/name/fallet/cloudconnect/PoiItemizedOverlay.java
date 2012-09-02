package name.fallet.cloudconnect;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

/**
 * Permet de positionner des éléments sur la carte
 * 
 * @author lfallet
 */
@SuppressWarnings("serial")
public class PoiItemizedOverlay extends ItemizedOverlay<OverlayItem> implements Serializable {

	private static final String TAG = PoiItemizedOverlay.class.getSimpleName();

	private final ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

	/** ne pas sérialiser le contexte! */
	private transient Context mContext;

	/**
	 * 
	 * @param defaultMarker
	 * @param context
	 *            nécessaire pour l'affichage de l'alerte
	 */
	public PoiItemizedOverlay(Drawable defaultMarker, Context context) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;

		// bug fix, pour empêcher les NullPointer au lancement d'un overlay vide
		populate();
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	/**
	 * la méthode populate() nécessite createItem()
	 */
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	/** Vide tous les overlay */
	public void clear() {
		mOverlays.clear();
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	/**
	 * Réagit à une interaction (tap) sur un OverlayItem
	 */
	@Override
	protected boolean onTap(int index) {
		// FIXME : je ne comprends pas pourquoi le tableau n'est pas bon
		if (mOverlays.size() < index) {
			Log.w(TAG, "Nombre d'overlays (" + mOverlays.size() + ") inférieur à l'index demandé : " + index);
			return false;
		}

		OverlayItem item = mOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		// TODO : positionner une icône différente selon la date de validité des données
		dialog.setIcon(mContext.getResources().getDrawable(R.drawable.executive_car_64x64));
		dialog.setNeutralButton(mContext.getResources().getText(R.string.fermer), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return true;
	}

}
