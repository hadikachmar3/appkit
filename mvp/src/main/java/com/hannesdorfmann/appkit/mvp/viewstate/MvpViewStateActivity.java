package com.hannesdorfmann.appkit.mvp.viewstate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.hannesdorfmann.appkit.mvp.MvpPresenter;
import com.hannesdorfmann.appkit.mvp.MvpView;
import com.hannesdorfmann.appkit.mvp.R;
import com.hannesdorfmann.appkit.mvp.animation.FadeHelper;
import com.hannesdorfmann.appkit.mvp.animation.MvpAnimator;
import icepick.Icepick;

/**
 * A Base activity.
 *
 * <p>
 * <b>Assumption:</b> There must be R.id.contentView, R.id.loadingView and R.id.errorView (type =
 * TextView)
 * specified in the inflated layout
 * </p>
 *
 * <p>
 * It already implements the default behaviours of {@link MvpView}.
 * For custom error messages you have to implement {@link #getErrorMessage(Exception, boolean)} and
 * for displaying error messages in a custom way you can override {@link #showLightError(String)}
 * </p>
 *
 * <p>
 * This class uses IcePick Annotation processing to make working with onSaveInstanceState() much
 * easier.
 * Simply use <code>@Icicle</code> to mark fields which state should be saved in
 * onSaveInstanceState().
 * </p>
 *
 * <p>
 * Uses Butterknife: So you can use Butterknifes Annotations in any subclass
 * </p>
 *
 * <p>
 * It supports ViewState. It means, that on orientation changes (screen rotation) the current view
 * state will be
 * stored and restored after orientation has changed. You can turn this feature off respectively on
 * by
 * returning false respectively true in {@link #isRetainingViewState()} (you have to override this
 * method). If you override {@link #showError(Exception, boolean)}, {@link #showContent()} or
 * {@link #showLoading(boolean)} than you have to keep in mind that you have to set the ViewState.
 * For this purpose you can use {@link #setErrorViewState(Exception, boolean)}, {@link
 * #setContentViewState()} and {@link #setLoadingViewState(boolean)}
 * </p>
 *
 * @param <AV> The type of the View (android view like ListView, FrameLayout etc.) that is
 * displayed
 * as content view.
 * @param <M> The data type that will by displayed in this Fragment
 * @param <V> The type this view inherited from the {@link MvpView} interface. <b>Note: </b> This
 * Fragment must also explicity implemnt this V interface. Otherwise a cast error may occure.
 * @param <P> The type of the Presenter
 * @author Hannes Dorfmann
 */
public abstract class MvpViewStateActivity<AV extends View, M, V extends MvpView<M>, P extends MvpPresenter<V, M>>
    extends ActionBarActivity implements MvpView<M> {

  /**
   * Get the ViewState
   *
   * @return
   */
  protected ViewState<M> viewState;

  protected AV contentView;

  protected TextView errorView;

  protected View loadingView;

  protected P presenter;

  @Override
  public void onCreate(Bundle saved) {

    super.onCreate(saved);
    Integer contentView = getContentViewLayoutRes();
    if (contentView != null) {
      super.setContentView(contentView);
    }

    Icepick.restoreInstanceState(this, saved);
    if (getIntent() != null) {
      Bundle extra = getIntent().getExtras();
      if (extra != null) {
        extractIntentExtra(extra);
      }
    }
    presenter = createPresenter(saved);
    presenter.setView((V) this);

    init(saved);

    if (!restoreViewState(saved)) {

      if (isRetainingViewState()) {
        viewState = createViewState();
        if (viewState == null) {
          throw new IllegalStateException("The ViewState can not be null! Return a valid ViewState "
              + "object from createViewState() or disable the ViewState feature by returning false "
              + "in isRetainingViewState()");
        }
      }

      loadData(false);
    }
    // otherwise the viewState would already be restored in restoreViewState();

  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Icepick.saveInstanceState(this, out);
    if (isRetainingViewState() && viewState != null) {
      viewState.saveInstanceState(out);
    }
  }

  /**
   * This method will be called to restore the previous view state
   *
   * @return true, if the viewState has been restored. Otherwise false
   */
  protected boolean restoreViewState(Bundle saved) {

    if (!isRetainingViewState()) {
      return false;
    }

    viewState = ViewState.restoreInstanceState(saved);
    if (viewState != null) {

      // Content was displayed
      if (viewState.wasShowingContent()) {
        M data = viewState.getLoadedData();
        setData(data);
        showContent();
        return true;
      }

      // Error was displayed
      if (viewState.wasShowingError()) {
        Exception exception = viewState.getException();
        // Restore previous data, if there was any
        if (viewState.getLoadedData() != null) {
          setData(viewState.getLoadedData());
          showContent();
        }
        showError(exception, viewState.isPullToRefresh());
        return true;
      }

      // Loading was displayed
      if (viewState.wasShowingLoading()) {

        // Restore previous data, if there was any
        if (viewState.getLoadedData() != null) {
          setData(viewState.getLoadedData());
          showContent();
        }

        showLoading(viewState.isPullToRefresh());
        loadData(viewState.isPullToRefresh());
        return true;
      }
    }

    return false;
  }

  /**
   * Create a new instance of a viewState. Choose here the correct ViewState implementation that
   * matches the requirements of the activity
   * This method will be called if the Activity is opened for the first time
   */
  public abstract ViewState<M> createViewState();

  /**
   * This method will be called from {@link #onCreate(Bundle)} for you to create a presenter
   */
  public abstract P createPresenter(Bundle savedInstanceState);

  /**
   * Use this method instead of setContenView(R.layout.my_layout).
   * It will automatically call setContentView() with this layout as resource as parameter for you.
   */
  public abstract Integer getContentViewLayoutRes();

  /**
   * Extracts the extras from the intent. Will be called from {@link #onCreate(Bundle)} and before
   * {@link #init(Bundle)}
   */
  public void extractIntentExtra(Bundle extra) {

  }

  /**
   * Load data by using the presenter. The presenter and view will be initialized
   */
  public abstract void loadData(boolean pullToRefresh);

  /**
   * This method will be called in {@link #onCreate(Bundle)} to allows you to do some addition
   * setup.
   * So heres the right place to initialize variables like adapters etc.
   *
   * <p>
   * This method will be called <b>after</b> {@link #getContentViewLayoutRes()},
   * {@link #extractIntentExtra(Bundle)} , {@link #createPresenter(Bundle)} ,{@link
   * #getContentViewLayoutRes()}.
   *
   * but <b>before</b> {@link #loadData(boolean)}
   * </p>
   */
  protected abstract void init(Bundle saved);

  /**
   * Return false if you don't want to use the whole ViewState mechanism at all.
   * Override this method and return false, if you want to disable the retaining ViewState
   * mechanism.
   *
   * @return true, if you want ViewState mechanism (i.e. for auto handling screen orientation
   * changes), otherwise false
   */
  public boolean isRetainingViewState() {
    return true;
  }

  @SuppressLint("WrongViewCast") @Override
  public void setContentView(int layoutResId) {
    super.setContentView(layoutResId);

    contentView = (AV) findViewById(R.id.contentView);
    errorView = (TextView) findViewById(R.id.errorView);
    loadingView = findViewById(R.id.loadingView);

    if (contentView == null) {
      throw new IllegalStateException("The content view is not specified. "
          + "You have to provide a View with R.id.contentView in your inflated xml layout");
    }

    if (loadingView == null) {
      throw new IllegalStateException("The loading view is not specified. "
          + "You have to provide a View with R.id.loadingView in your inflated xml layout");
    }

    if (errorView == null) {
      throw new IllegalStateException("The error view is not specified. "
          + "You have to provide a View with R.id.errorView in your inflated xml layout");
    }

    errorView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onErrorViewClicked();
      }
    });
  }

  /**
   * Called if the user clicks on the error view
   */
  protected void onErrorViewClicked() {
    loadData(false);
  }

  /**
   * Get the presenter that is used. This is one will be used automatically call
   * {@link MvpPresenter#onDestroy(boolean)} for you at correct time and
   * place.
   * So you don't have to care about it
   */
  protected P getPresenter() {
    return presenter;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (getPresenter() != null) {
      getPresenter().onDestroy(false);
    }
  }

  /**
   * This method should be called in {@link #showLoading(boolean)}
   */
  protected void setLoadingViewState(boolean pullToRefresh) {
    if (!isRetainingViewState()) {
      return;
    }

    viewState.setStateShowLoading(pullToRefresh);
  }

  /**
   * This method should be called in {@link #showContent()}
   */
  protected void setContentViewState() {
    if (!isRetainingViewState()) {
      return;
    }

    viewState.setStateShowContent(getData());
  }

  /**
   * This method should be called in {@link #showError(Exception, boolean)}
   */
  protected void setErrorViewState(Exception e, boolean pullToRefresh) {
    if (!isRetainingViewState()) {
      return;
    }

    viewState.setStateShowError(e, pullToRefresh);
  }

  @Override
  public void showLoading(boolean pullToRefresh) {

    setLoadingViewState(pullToRefresh);

    if (!pullToRefresh) {
      animateLoadingViewIn();
    }
    // swipe refresh layout will be triggered
    // with a gesture and indicator is already visible
  }

  /**
   * Animates the loading view in (replaces error view or content view)
   */
  protected void animateLoadingViewIn() {
    FadeHelper.showLoading(loadingView, contentView, errorView);
  }

  @Override
  public void showContent() {

    setContentViewState();
    animateContentViewIn();
  }

  /**
   * Animates the content view in (replaces error view or loading view)
   */
  protected void animateContentViewIn() {
    MvpAnimator.showContent(loadingView, contentView, errorView);
  }

  /**
   * /**
   * The default behaviour is to display a toast message as light error (i.e. pull-to-refresh
   * error).
   * Override this method if you want to display the light error in another way (like crouton).
   */
  protected void showLightError(String msg) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  }

  /**
   * Get the error message for a certain Exception that will be shown on {@link
   * #showError(Exception, boolean)}
   */
  protected abstract String getErrorMessage(Exception e, boolean pullToRefresh);

  @Override
  public void showError(Exception e, boolean pullToRefresh) {
    setErrorViewState(e, pullToRefresh);
    String errorMsg = getErrorMessage(e, pullToRefresh);
    if (pullToRefresh) {
      showLightError(errorMsg);
    } else {
      errorView.setText(errorMsg);
    }
  }

  /**
   * Animates the error view in (replaces loading view or content view)
   */
  protected void animateErrorViewIn() {
    FadeHelper.showErrorView(loadingView, contentView, errorView);
  }
}
