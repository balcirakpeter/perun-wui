package cz.metacentrum.perun.wui.consolidator.client;

import com.google.gwt.core.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import cz.metacentrum.perun.wui.client.resources.PerunSession;
import cz.metacentrum.perun.wui.client.utils.Utils;
import cz.metacentrum.perun.wui.consolidator.widgets.Wayf;
import cz.metacentrum.perun.wui.json.JsonEvents;
import cz.metacentrum.perun.wui.json.managers.RegistrarManager;
import cz.metacentrum.perun.wui.model.BasicOverlayObject;
import cz.metacentrum.perun.wui.model.PerunException;
import cz.metacentrum.perun.wui.model.beans.ExtSource;
import cz.metacentrum.perun.wui.widgets.*;
import org.gwtbootstrap3.client.ui.*;

/**
 * Single page used by consolidator to display it's state
 *
 * @author Pavel Zlámal <zlamal@cesnet.cz>
 */
public class SelectPage {

	private String token;
	private String redirect = Window.Location.getParameter("target_url");
	private Widget rootElement;

	interface ConsolidatorPageUiBinder extends UiBinder<Widget, SelectPage> {
	}

	private static ConsolidatorPageUiBinder ourUiBinder = GWT.create(ConsolidatorPageUiBinder.class);

	private ConsolidatorTranslation translation = GWT.create(ConsolidatorTranslation.class);

	@UiField(provided = true) Wayf wayf;

	@UiField PerunLoader loader;
	@UiField Heading heading;
	@UiField Heading joinHeading;
	@UiField Heading identity;
	@UiField Alert alert;

	public SelectPage() {
	}

	public Widget draw() {

		wayf = new Wayf(null, redirect);

		if (rootElement == null) {
			rootElement = ourUiBinder.createAndBindUi(this);
		}

		heading.setText(translation.currentIdentityIs());
		joinHeading.setText(translation.joinWith());

		final JsonEvents loadWayfEvent = new JsonEvents() {
			@Override
			public void onFinished(JavaScriptObject jso) {
				loader.onFinished();
				loader.setVisible(false);
			}

			@Override
			public void onError(PerunException error) {
				loader.onError(error, null);
			}

			@Override
			public void onLoadingStart() {

			}
		};

		if (token == null || token.isEmpty()) {

			RegistrarManager.getConsolidatorToken(new JsonEvents() {
				@Override
				public void onFinished(JavaScriptObject jso) {

					token = ((BasicOverlayObject) jso).getString();

					// we do have a valid token
					String extSourceType = PerunSession.getInstance().getPerunPrincipal().getExtSourceType();
					String translatedExtSourceName = PerunSession.getInstance().getPerunPrincipal().getExtSource();
					String translatedActor = PerunSession.getInstance().getPerunPrincipal().getActor();

					if (extSourceType.equals(ExtSource.ExtSourceType.IDP.getType())) {
						translatedExtSourceName = Utils.translateIdp(translatedExtSourceName);
						// social identity
						if (translatedActor.endsWith("extidp.cesnet.cz")) {
							translatedExtSourceName = Utils.translateIdp("@"+translatedActor.split("@")[1]);
							translatedActor = translatedActor.split("@")[0];
						}
					} else if (extSourceType.equals(ExtSource.ExtSourceType.X509.getType())) {
						translatedActor = Utils.convertCertCN(translatedActor);
						translatedExtSourceName = Utils.convertCertCN(translatedExtSourceName);
					}

					heading.setVisible(true);
					identity.setText(translatedActor);
					identity.setVisible(true);
					joinHeading.setVisible(true);

					identity.setSubText(translatedExtSourceName);

					if (PerunSession.getInstance().getUser() == null) {
						alert.setVisible(true);
						alert.setText(translation.notRegistered());
					}

					wayf.setToken(token);
					wayf.loadWayf(loadWayfEvent);

				}

				@Override
				public void onError(PerunException error) {
					loader.onError(error, null);
				}

				@Override
				public void onLoadingStart() {
					loader.onLoading();
					loader.setVisible(true);
				}
			});

		} else {

			wayf.loadWayf(loadWayfEvent);

		}

		return rootElement;

	}

}
