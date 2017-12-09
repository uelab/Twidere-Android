/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.fragment

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter.CreateNdefMessageCallback
import android.os.Build
import android.os.Bundle
import android.support.annotation.*
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.pm.ShortcutManagerCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.ColorUtils
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v4.view.WindowCompat
import android.support.v4.widget.ImageViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.util.Linkify
import android.util.SparseBooleanArray
import android.view.*
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.header_user.*
import kotlinx.android.synthetic.main.header_user.view.*
import kotlinx.android.synthetic.main.layout_content_fragment_common.*
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.mariotaku.chameleon.Chameleon
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.*
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.MicroBlogException
import org.mariotaku.microblog.library.twitter.model.FriendshipUpdate
import org.mariotaku.microblog.library.twitter.model.Paging
import org.mariotaku.microblog.library.twitter.model.UserList
import org.mariotaku.twidere.Constants.*
import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.AccountSelectorActivity
import org.mariotaku.twidere.activity.BaseActivity
import org.mariotaku.twidere.activity.ColorPickerDialogActivity
import org.mariotaku.twidere.activity.LinkHandlerActivity
import org.mariotaku.twidere.activity.iface.IBaseActivity
import org.mariotaku.twidere.activity.iface.IControlBarActivity
import org.mariotaku.twidere.adapter.SupportTabsAdapter
import org.mariotaku.twidere.annotation.AccountType
import org.mariotaku.twidere.annotation.DisplayOption
import org.mariotaku.twidere.annotation.TimelineStyle
import org.mariotaku.twidere.constant.*
import org.mariotaku.twidere.constant.KeyboardShortcutConstants.*
import org.mariotaku.twidere.data.impl.RelationshipLiveData
import org.mariotaku.twidere.data.impl.UserLiveData
import org.mariotaku.twidere.extension.*
import org.mariotaku.twidere.extension.model.*
import org.mariotaku.twidere.extension.model.api.microblog.toParcelable
import org.mariotaku.twidere.fragment.iface.IBaseFragment.SystemWindowInsetsCallback
import org.mariotaku.twidere.fragment.iface.IToolBarSupportFragment
import org.mariotaku.twidere.fragment.iface.RefreshScrollTopInterface
import org.mariotaku.twidere.fragment.iface.SupportFragmentCallback
import org.mariotaku.twidere.fragment.timeline.AbsTimelineFragment
import org.mariotaku.twidere.fragment.timeline.FavoritesTimelineFragment
import org.mariotaku.twidere.fragment.timeline.UserMediaTimelineFragment
import org.mariotaku.twidere.fragment.timeline.UserTimelineFragment
import org.mariotaku.twidere.graphic.ActionIconDrawable
import org.mariotaku.twidere.graphic.drawable.userprofile.ActionBarDrawable
import org.mariotaku.twidere.model.*
import org.mariotaku.twidere.model.event.FriendshipTaskEvent
import org.mariotaku.twidere.model.event.FriendshipUpdatedEvent
import org.mariotaku.twidere.model.event.ProfileUpdatedEvent
import org.mariotaku.twidere.model.event.TaskStateChangedEvent
import org.mariotaku.twidere.model.theme.UserTheme
import org.mariotaku.twidere.model.util.ParcelableMediaUtils
import org.mariotaku.twidere.promise.BlockPromises
import org.mariotaku.twidere.promise.FriendshipPromises
import org.mariotaku.twidere.promise.MutePromises
import org.mariotaku.twidere.promise.UserListPromises
import org.mariotaku.twidere.provider.TwidereDataStore.CachedRelationships
import org.mariotaku.twidere.provider.TwidereDataStore.CachedUsers
import org.mariotaku.twidere.task.UpdateAccountInfoPromise
import org.mariotaku.twidere.text.TwidereURLSpan
import org.mariotaku.twidere.util.*
import org.mariotaku.twidere.util.KeyboardShortcutsHandler.KeyboardShortcutCallback
import org.mariotaku.twidere.util.TwidereLinkify.OnLinkClickListener
import org.mariotaku.twidere.util.UserColorNameManager.UserColorChangedListener
import org.mariotaku.twidere.util.UserColorNameManager.UserNicknameChangedListener
import org.mariotaku.twidere.util.menu.TwidereMenuInfo
import org.mariotaku.twidere.util.shortcut.ShortcutCreator
import org.mariotaku.twidere.util.support.ActivitySupport
import org.mariotaku.twidere.util.support.ActivitySupport.TaskDescriptionCompat
import org.mariotaku.twidere.util.support.ViewSupport
import org.mariotaku.twidere.util.support.WindowSupport
import org.mariotaku.twidere.util.support.view.ViewOutlineProviderCompat
import java.util.*

class UserFragment : BaseFragment(), OnClickListener, OnLinkClickListener,
        OnTouchListener, SupportFragmentCallback, LinkHandlerActivity.HideUiOnScroll,
        SystemWindowInsetsCallback, RefreshScrollTopInterface, OnPageChangeListener,
        KeyboardShortcutCallback, UserColorChangedListener, UserNicknameChangedListener,
        IToolBarSupportFragment, AbsContentRecyclerViewFragment.RefreshCompleteListener {

    override val currentVisibleFragment: Fragment?
        get() {
            val currentItem = viewPager.currentItem
            if (currentItem < 0 || currentItem >= pagerAdapter.count) return null
            return pagerAdapter.instantiateItem(viewPager, currentItem)
        }

    override val fragmentToolbar: Toolbar
        get() = toolbar

    override val controlBarHeight: Int
        get() = toolbar.height

    override var controlBarOffset: Float
        get() = 1 + toolbar.translationY / 0.98f / controlBarHeight
        set(offset) {
            val translationY = (offset - 1) * controlBarHeight
            toolbar.translationY = translationY * 0.98f
            profileHeader.translationY = translationY
            tabsShadow.translationY = translationY
        }

    private val keyboardShortcutRecipient: Fragment?
        get() = currentVisibleFragment

    private lateinit var profileBirthdayBanner: View
    private lateinit var pagerAdapter: SupportTabsAdapter

    // Data fields
    private lateinit var liveUser: UserLiveData
    private lateinit var liveRelationship: RelationshipLiveData

    private var cardBackgroundColor: Int = 0
    private var actionBarShadowColor: Int = 0
    private var uiColor: Int = 0
    private var primaryColor: Int = 0
    private var nameFirst: Boolean = false
    private var hideBirthdayView: Boolean = false

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        updateSubtitle()
    }

    override fun onPageScrollStateChanged(state: Int) {
        (activity as? IControlBarActivity)?.setControlBarVisibleAnimate(true)
    }

    override fun triggerRefresh(position: Int): Boolean {
        return false
    }

    override fun getSystemWindowInsets(caller: Fragment, insets: Rect): Boolean {
        insetsCallback?.getSystemWindowInsets(this, insets)
        if (caller.parentFragment === this) {
            insets.top = toolbar.measuredHeight + toolbarTabs.measuredHeight
        }
        return true
    }

    @Subscribe
    fun notifyFriendshipUpdated(event: FriendshipUpdatedEvent) {
        val user = liveUser.user
        if (user == null || !event.isAccount(user.account_key) || !event.isUser(user.key.id))
            return
        getFriendship()
    }

    @Subscribe
    fun notifyFriendshipUserUpdated(event: FriendshipTaskEvent) {
        val user = liveUser.user
        if (user == null || !event.isSucceeded || !event.isUser(user)) return
        getFriendship()
    }

    @Subscribe
    fun notifyProfileUpdated(event: ProfileUpdatedEvent) {
        val user = liveUser.user
        // TODO check account status
        if (user == null || user != event.user) return
        displayUser(event.user)
    }

    @Subscribe
    fun notifyTaskStateChanged(event: TaskStateChangedEvent) {
        activity?.invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val user = liveUser.user ?: return
        val accountKey = user.account_key ?: return
        when (requestCode) {
            REQUEST_SET_COLOR -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) return
                    val color = data.getIntExtra(EXTRA_COLOR, Color.TRANSPARENT)
                    userColorNameManager.setUserColor(user.key, color)
                } else if (resultCode == ColorPickerDialogActivity.RESULT_CLEARED) {
                    userColorNameManager.clearUserColor(user.key)
                }
            }
            REQUEST_ADD_TO_LIST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val list = data.extras.userList!!
                    UserListPromises.get(context!!).addMembers(accountKey, list.id, user)
                }
            }
            REQUEST_SELECT_ACCOUNT -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || !data.hasExtra(EXTRA_ID)) return
                    val selectedAccountKey: UserKey = data.getParcelableExtra(EXTRA_ACCOUNT_KEY) ?: return
                    var userKey = user.key
                    if (liveUser.account?.type == AccountType.MASTODON && liveUser.account?.key?.host != selectedAccountKey.host) {
                        userKey = AcctPlaceholderUserKey(user.key.host)
                    }
                    IntentUtils.openUserProfile(context!!, selectedAccountKey, userKey, user.screen_name,
                            user.extras?.statusnet_profile_url, preferences[newDocumentApiKey],
                            null)
                }
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        linkHandlerTitle = null
        val activity = activity!!
        val args = arguments!!

        nameFirst = preferences[nameFirstKey]
        cardBackgroundColor = ThemeUtils.getCardBackgroundColor(activity,
                preferences[themeBackgroundOptionKey], preferences[themeBackgroundAlphaKey])
        actionBarShadowColor = 0xA0000000.toInt()
        val accountKey = args.accountKey ?: run {
            activity.finish()
            return
        }

        Utils.setNdefPushMessageCallback(activity, CreateNdefMessageCallback {
            val user = liveUser.user ?: return@CreateNdefMessageCallback null
            NdefMessage(arrayOf(NdefRecord.createUri(LinkCreator.getUserWebLink(user))))
        })

        pagerAdapter = SupportTabsAdapter(activity, childFragmentManager)
        liveUser = UserLiveData(activity, accountKey, args.userKey, args.screenName)
        liveRelationship = RelationshipLiveData(activity, accountKey)

        viewPager.offscreenPageLimit = 3
        viewPager.adapter = pagerAdapter
        toolbarTabs.setViewPager(viewPager)
        toolbarTabs.setTabDisplayOption(DisplayOption.LABEL)
        toolbarTabs.setOnPageChangeListener(this)

        followContainer.follow.setOnClickListener(this)
        profileImage.setOnClickListener(this)
        profileBanner.setOnClickListener(this)
        listedCount.setOnClickListener(this)
        groupsCount.setOnClickListener(this)
        followersCount.setOnClickListener(this)
        friendsCount.setOnClickListener(this)
        url.setOnClickListener(this)
        profileBannerSpace.setOnTouchListener(this)

        profileHeaderBackground.setBackgroundColor(cardBackgroundColor)
        toolbarTabs.setBackgroundColor(cardBackgroundColor)

        setupViewStyle()
        setupViewSettings()
        setupUserPages()

        liveUser.observe(this, success = { (account, user) ->
            errorContainer.visibility = View.GONE
            progressContainer.visibility = View.GONE
            displayUser(user)
            updateOptionsMenuVisibility()
            if (liveRelationship.value == null) {
                getFriendship()
            }
            if (user.is_cache) {
                getUserInfo(true)
            } else {
                UpdateAccountInfoPromise(context!!).promise(account, user)
            }
        }, fail = { exception ->
            errorText.text = exception.getErrorMessage(activity)
            errorText.visibility = View.VISIBLE
            errorContainer.visibility = View.VISIBLE
            progressContainer.visibility = View.GONE
            setContentVisible(false)
            updateOptionsMenuVisibility()
        })
        liveRelationship.observe(this, success = { relationship ->
            followProgress.visibility = View.GONE
            displayRelationship(relationship)
            updateOptionsMenuVisibility()
        }, fail = {
            followContainer.follow.visibility = View.GONE
            followingYouIndicator.visibility = View.GONE
            followProgress.visibility = View.VISIBLE
        })

        getUserInfo(false)
    }

    override fun onStart() {
        super.onStart()
        bus.register(this)
        userColorNameManager.registerColorChangedListener(this)
        userColorNameManager.registerNicknameChangedListener(this)
    }


    override fun onStop() {
        userColorNameManager.unregisterColorChangedListener(this)
        userColorNameManager.unregisterNicknameChangedListener(this)
        bus.unregister(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setUiColor(uiColor)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_USER, liveUser.user)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_user_profile, menu)
    }

    @UiThread
    override fun onPrepareOptionsMenu(menu: Menu) {
        val context = context ?: return
        val user = this.liveUser.user ?: return
        val account = this.liveUser.account
        val relationship = this.liveRelationship.relationship

        val isMyself = user.isSelf
        val mentionItem = menu.findItem(R.id.mention)
        if (mentionItem != null) {
            val displayName = UserColorNameManager.decideDisplayName(user.nickname,
                    user.name, user.screen_name, nameFirst)
            mentionItem.title = getString(R.string.mention_user_name, displayName)
        }
        menu.setItemAvailability(R.id.mention, !isMyself)
        menu.setItemAvailability(R.id.incoming_friendships, isMyself)
        menu.setItemAvailability(R.id.saved_searches, isMyself)

        menu.setItemAvailability(R.id.blocked_users, isMyself)
        menu.setItemAvailability(R.id.block, !isMyself)

        menu.setItemAvailability(R.id.add_to_home_screen_submenu,
                ShortcutManagerCompat.isRequestPinShortcutSupported(context))

        var canAddToList = false
        var canMute = false
        var canReportSpam = false
        var canEnableRetweet = false
        var canEnableNotifications = false
        when (account?.type) {
            AccountType.TWITTER -> {
                canAddToList = true
                canMute = true
                canReportSpam = true
                canEnableRetweet = true
                canEnableNotifications = true
            }
            AccountType.MASTODON -> {
                canMute = true
            }
        }

        menu.setItemAvailability(R.id.add_to_list, canAddToList)
        menu.setItemAvailability(R.id.mute_user, !isMyself && canMute)
        menu.setItemAvailability(R.id.muted_users, isMyself && canMute)
        menu.setItemAvailability(R.id.report_spam, !isMyself && canReportSpam)
        menu.setItemAvailability(R.id.enable_retweets, !isMyself && canEnableRetweet)

        if (relationship != null) {
            menu.findItem(R.id.add_to_filter)?.apply {
                isChecked = relationship.filtering
            }

            if (isMyself) {
                menu.setItemAvailability(R.id.send_direct_message, false)
                menu.setItemAvailability(R.id.enable_notifications, false)
            } else {
                menu.setItemAvailability(R.id.send_direct_message, relationship.can_dm)
                menu.setItemAvailability(R.id.block, true)
                menu.setItemAvailability(R.id.enable_notifications, canEnableNotifications &&
                        relationship.following)

                menu.findItem(R.id.block)?.apply {
                    ActionIconDrawable.setMenuHighlight(this, TwidereMenuInfo(relationship.blocking))
                    this.setTitle(if (relationship.blocking) R.string.action_unblock else R.string.action_block)
                }
                menu.findItem(R.id.mute_user)?.apply {
                    isChecked = relationship.muting
                }
                menu.findItem(R.id.enable_retweets)?.apply {
                    isChecked = relationship.retweet_enabled
                }
                menu.findItem(R.id.enable_notifications)?.apply {
                    isChecked = relationship.notifications_enabled
                }
            }

        } else {
            menu.setItemAvailability(R.id.send_direct_message, false)
            menu.setItemAvailability(R.id.enable_notifications, false)
        }
        val intent = Intent(INTENT_ACTION_EXTENSION_OPEN_USER)
        val extras = Bundle()
        extras.putParcelable(EXTRA_USER, user)
        intent.putExtras(extras)
        menu.removeGroup(MENU_GROUP_USER_EXTENSION)
        MenuUtils.addIntentToMenu(context, menu, intent, MENU_GROUP_USER_EXTENSION)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val context = context ?: return false
        val fragmentManager = fragmentManager ?: return false
        val user = liveUser.user ?: return false
        val accountKey = user.account_key ?: return false
        val userRelationship = liveRelationship.relationship
        when (item.itemId) {
            R.id.block -> {
                if (userRelationship == null) return true
                if (userRelationship.blocking) {
                    BlockPromises.getInstance(context).unblock(accountKey, user.key)
                } else {
                    CreateUserBlockDialogFragment.show(fragmentManager, user)
                }
            }
            R.id.report_spam -> {
                ReportUserSpamDialogFragment.show(fragmentManager, user)
            }
            R.id.add_to_filter -> {
                if (userRelationship == null) return true
                if (userRelationship.filtering) {
                    DataStoreUtils.removeFromFilter(context, listOf(user))
                    Toast.makeText(activity, R.string.message_toast_user_filters_removed,
                            Toast.LENGTH_SHORT).show()
                    getFriendship()
                } else {
                    AddUserFilterDialogFragment.show(fragmentManager, user)
                }
            }
            R.id.mute_user -> {
                if (userRelationship == null) return true
                if (userRelationship.muting) {
                    MutePromises.get(context).unmute(accountKey, user.key)
                } else {
                    CreateUserMuteDialogFragment.show(fragmentManager, user)
                }
            }
            R.id.mention -> {
                val intent = Intent(INTENT_ACTION_MENTION)
                val bundle = Bundle()
                bundle.putParcelable(EXTRA_USER, user)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            R.id.send_direct_message -> {
                val am = AccountManager.get(activity)
                val builder = Uri.Builder().apply {
                    scheme(SCHEME_TWIDERE)
                    authority(AUTHORITY_MESSAGES)
                    path(PATH_MESSAGES_CONVERSATION_NEW)
                    appendQueryParameter(QUERY_PARAM_ACCOUNT_KEY, accountKey.toString())
                }
                val intent = Intent(Intent.ACTION_VIEW, builder.build())
                intent.putExtra(EXTRA_ACCOUNT, am.getDetails(accountKey, true))
                intent.putExtra(EXTRA_USERS, arrayOf(user))
                intent.putExtra(EXTRA_OPEN_CONVERSATION, true)
                startActivity(intent)
            }
            R.id.set_color -> {
                val intent = Intent(activity, ColorPickerDialogActivity::class.java)
                intent.putExtra(EXTRA_COLOR, userColorNameManager.getUserColor(user.key))
                intent.putExtra(EXTRA_ALPHA_SLIDER, false)
                intent.putExtra(EXTRA_CLEAR_BUTTON, true)
                startActivityForResult(intent, REQUEST_SET_COLOR)
            }
            R.id.clear_nickname -> {
                userColorNameManager.clearUserNickname(user.key)
            }
            R.id.set_nickname -> {
                val nick = userColorNameManager.getUserNickname(user.key)
                SetUserNicknameDialogFragment.show(fragmentManager, user.key, nick)
            }
            R.id.add_to_list -> {
                showAddToListDialog(user)
            }
            R.id.open_with_account -> {
                val intent = Intent(INTENT_ACTION_SELECT_ACCOUNT)
                intent.setClass(activity, AccountSelectorActivity::class.java)
                intent.putExtra(EXTRA_SINGLE_SELECTION, true)
                when (liveUser.account?.type) {
                    AccountType.MASTODON -> intent.putExtra(EXTRA_ACCOUNT_TYPE, AccountType.MASTODON)
                    else -> intent.putExtra(EXTRA_ACCOUNT_HOST, user.key.host)
                }
                startActivityForResult(intent, REQUEST_SELECT_ACCOUNT)
            }
            R.id.follow -> {
                if (userRelationship == null) return true
                val updatingRelationship = FriendshipPromises.isRunning(accountKey,
                        user.key)
                if (!updatingRelationship) {
                    if (userRelationship.following) {
                        DestroyFriendshipDialogFragment.show(fragmentManager, user)
                    } else {
                        FriendshipPromises.getInstance(context).create(accountKey, user.key, user.screen_name)
                    }
                }
                return true
            }
            R.id.enable_retweets -> {
                val newState = !item.isChecked
                val update = FriendshipUpdate()
                update.retweets(newState)
                FriendshipPromises.get(context).update(accountKey, user.key, update)
                item.isChecked = newState
                return true
            }
            R.id.enable_notifications -> {
                val newState = !item.isChecked
                if (newState) {
                    Toast.makeText(context, R.string.message_toast_notification_enabled_hint,
                            Toast.LENGTH_SHORT).show()
                }
                val update = FriendshipUpdate()
                update.deviceNotifications(newState)
                FriendshipPromises.get(context).update(accountKey, user.key, update)
                item.isChecked = newState
                return true
            }
            R.id.muted_users -> {
                IntentUtils.openMutesUsers(context, accountKey)
                return true
            }
            R.id.blocked_users -> {
                IntentUtils.openUserBlocks(context, accountKey)
                return true
            }
            R.id.incoming_friendships -> {
                IntentUtils.openIncomingFriendships(context, accountKey)
                return true
            }
            R.id.user_mentions -> {
                IntentUtils.openUserMentions(context, accountKey, user.screen_name)
                return true
            }
            R.id.saved_searches -> {
                IntentUtils.openSavedSearches(context, accountKey)
                return true
            }
            R.id.open_in_browser -> {
                val uri = LinkCreator.getUserWebLink(user)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.`package` = IntentUtils.getDefaultBrowserPackage(context, uri, true)
                if (intent.resolveActivity(context.packageManager) != null) {
                    startActivity(intent)
                }
                return true
            }
            R.id.qr_code -> {
                executeAfterFragmentResumed {
                    val df = UserQrDialogFragment()
                    df.arguments = Bundle {
                        this[EXTRA_USER] = user
                    }
                    df.show(it.childFragmentManager, "user_qr_code")
                }
                return true
            }
            R.id.add_user_to_home_screen -> {
                ShortcutCreator.performCreation(this) {
                    ShortcutCreator.user(context, accountKey, user)
                }
            }
            R.id.add_statuses_to_home_screen -> {
                ShortcutCreator.performCreation(this) {
                    ShortcutCreator.userTimeline(context, accountKey, user)
                }
            }
            R.id.add_favorites_to_home_screen -> {
                ShortcutCreator.performCreation(this) {
                    ShortcutCreator.userFavorites(context, accountKey, user)
                }
            }
            R.id.add_media_to_home_screen -> {
                ShortcutCreator.performCreation(this) {
                    ShortcutCreator.userMediaTimeline(context, accountKey, user)
                }
            }
            else -> {
                val intent = item.intent
                if (intent?.resolveActivity(context.packageManager) != null) {
                    startActivity(intent)
                }
            }
        }
        return true
    }


    override fun handleKeyboardShortcutSingle(handler: KeyboardShortcutsHandler, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        if (handleFragmentKeyboardShortcutSingle(handler, keyCode, event, metaState)) return true
        val action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState)
        if (action != null) {
            when (action) {
                ACTION_NAVIGATION_PREVIOUS_TAB -> {
                    val previous = viewPager.currentItem - 1
                    if (previous >= 0 && previous < pagerAdapter.count) {
                        viewPager.setCurrentItem(previous, true)
                    }
                    return true
                }
                ACTION_NAVIGATION_NEXT_TAB -> {
                    val next = viewPager.currentItem + 1
                    if (next >= 0 && next < pagerAdapter.count) {
                        viewPager.setCurrentItem(next, true)
                    }
                    return true
                }
            }
        }
        return handler.handleKey(activity, null, keyCode, event, metaState)
    }

    override fun isKeyboardShortcutHandled(handler: KeyboardShortcutsHandler, keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        if (isFragmentKeyboardShortcutHandled(handler, keyCode, event, metaState)) return true
        val action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event, metaState)
        if (action != null) {
            when (action) {
                ACTION_NAVIGATION_PREVIOUS_TAB, ACTION_NAVIGATION_NEXT_TAB -> return true
            }
        }
        return false
    }

    override fun handleKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler,
            keyCode: Int, repeatCount: Int,
            event: KeyEvent, metaState: Int): Boolean {
        return handleFragmentKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState)
    }

    private fun updateSubtitle() {
        val activity = activity as AppCompatActivity
        val actionBar = activity.supportActionBar ?: return
        val user = this.liveUser.user
        if (user == null) {
            actionBar.subtitle = null
            return
        }
        val spec = pagerAdapter.get(viewPager.currentItem)
        assert(spec.type != null)
        when (spec.type) {
            TAB_TYPE_STATUSES, TAB_TYPE_STATUSES_WITH_REPLIES -> {
                actionBar.subtitle = resources.getQuantityString(R.plurals.N_statuses,
                        user.statuses_count.toInt(), user.statuses_count)
            }
            TAB_TYPE_MEDIA -> {
                if (user.media_count < 0) {
                    actionBar.setSubtitle(R.string.recent_media)
                } else {
                    actionBar.subtitle = resources.getQuantityString(R.plurals.N_media,
                            user.media_count.toInt(), user.media_count)
                }
            }
            TAB_TYPE_FAVORITES -> {
                if (user.favorites_count < 0) {
                    if (preferences[iWantMyStarsBackKey]) {
                        actionBar.setSubtitle(R.string.title_favorites)
                    } else {
                        actionBar.setSubtitle(R.string.title_likes)
                    }
                } else if (preferences[iWantMyStarsBackKey]) {
                    actionBar.subtitle = resources.getQuantityString(R.plurals.N_favorites,
                            user.favorites_count.toInt(), user.favorites_count)
                } else {
                    actionBar.subtitle = resources.getQuantityString(R.plurals.N_likes,
                            user.favorites_count.toInt(), user.favorites_count)
                }
            }
            else -> {
                actionBar.subtitle = null
            }
        }
    }

    private fun handleFragmentKeyboardShortcutRepeat(handler: KeyboardShortcutsHandler,
            keyCode: Int, repeatCount: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event, metaState)
        }
        return false
    }

    private fun handleFragmentKeyboardShortcutSingle(handler: KeyboardShortcutsHandler,
            keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.handleKeyboardShortcutSingle(handler, keyCode, event, metaState)
        }
        return false
    }

    private fun isFragmentKeyboardShortcutHandled(handler: KeyboardShortcutsHandler,
            keyCode: Int, event: KeyEvent, metaState: Int): Boolean {
        val fragment = keyboardShortcutRecipient
        if (fragment is KeyboardShortcutCallback) {
            return fragment.isKeyboardShortcutHandled(handler, keyCode, event, metaState)
        }
        return false
    }

    override fun onApplySystemWindowInsets(insets: Rect) {
    }

    override fun setupWindow(activity: FragmentActivity): Boolean {
        if (activity is AppCompatActivity) {
            activity.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            activity.supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_MODE_OVERLAY)
        }
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.requestFeature(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        WindowSupport.setStatusBarColor(window, Color.TRANSPARENT)
        return true
    }

    override fun onClick(view: View) {
        val context = activity ?: return
        val fragmentManager = fragmentManager ?: return
        val user = liveUser.user ?: return
        val accountKey = user.account_key ?: return
        when (view.id) {
            R.id.errorContainer -> {
                getUserInfo(true)
            }
            R.id.follow -> {
                if (accountKey.maybeEquals(user.key)) {
                    IntentUtils.openProfileEditor(context, accountKey)
                } else {
                    val userRelationship = liveRelationship.relationship ?: return
                    when {
                        userRelationship.blocking -> {
                            BlockPromises.getInstance(context).unblock(accountKey, user.key)
                        }
                        userRelationship.blocked_by -> {
                            CreateUserBlockDialogFragment.show(childFragmentManager, user)
                        }
                        userRelationship.following -> {
                            DestroyFriendshipDialogFragment.show(fragmentManager, user)
                        }
                        else -> {
                            FriendshipPromises.getInstance(context).create(accountKey, user.key, user.screen_name)
                        }
                    }
                }
            }
            R.id.profileImage -> {
                val url = user.originalProfileImage ?: return
                val profileImage = ParcelableMediaUtils.image(url)
                profileImage.type = ParcelableMedia.Type.IMAGE
                profileImage.preview_url = user.profile_image_url
                val media = arrayOf(profileImage)
                IntentUtils.openMedia(context, accountKey, media, null, false,
                        preferences[newDocumentApiKey], preferences[displaySensitiveContentsKey])
            }
            R.id.profileBanner -> {
                val url = user.getBestProfileBanner(0) ?: return
                val profileBanner = ParcelableMediaUtils.image(url)
                profileBanner.type = ParcelableMedia.Type.IMAGE
                val media = arrayOf(profileBanner)
                IntentUtils.openMedia(context, accountKey, media, null, false,
                        preferences[newDocumentApiKey], preferences[displaySensitiveContentsKey])
            }
            R.id.listedCount -> {
                IntentUtils.openUserLists(context, accountKey, user.key, user.screen_name)
            }
            R.id.groupsCount -> {
                IntentUtils.openUserGroups(context, accountKey, user.key, user.screen_name)
            }
            R.id.followersCount -> {
                IntentUtils.openUserFollowers(context, accountKey, user.key, user.screen_name)
            }
            R.id.friendsCount -> {
                IntentUtils.openUserFriends(context, accountKey, user.key, user.screen_name)
            }
            R.id.nameContainer -> {
                if (accountKey == user.key) return
                IntentUtils.openProfileEditor(context, accountKey)
            }
            R.id.url -> {
                val uri = user.urlPreferred?.let(Uri::parse) ?: return
                OnLinkClickHandler.openLink(context, preferences, uri)
            }
            R.id.profileBirthdayBanner -> {
                hideBirthdayView = true
                profileBirthdayBanner.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out))
                profileBirthdayBanner.visibility = View.GONE
            }
        }

    }

    override fun onLinkClick(link: String, orig: String?, accountKey: UserKey?,
            extraId: Long, type: Int, sensitive: Boolean,
            start: Int, end: Int): Boolean {
        val activity = activity ?: return false
        val user = liveUser.user ?: return false
        when (type) {
            TwidereLinkify.LINK_TYPE_MENTION -> {
                IntentUtils.openUserProfile(activity, user.account_key, null, link, null,
                        preferences[newDocumentApiKey], null)
                return true
            }
            TwidereLinkify.LINK_TYPE_HASHTAG -> {
                IntentUtils.openTweetSearch(activity, user.account_key, "#" + link)
                return true
            }
            TwidereLinkify.LINK_TYPE_LINK_IN_TEXT, TwidereLinkify.LINK_TYPE_ENTITY_URL -> {
                val uri = Uri.parse(link)
                val intent: Intent
                if (uri.scheme != null) {
                    intent = Intent(Intent.ACTION_VIEW, uri)
                } else {
                    intent = Intent(Intent.ACTION_VIEW, uri.buildUpon().scheme("http").build())
                }
                startActivity(intent)
                return true
            }
            TwidereLinkify.LINK_TYPE_LIST -> {
                val mentionList = link.split("/").dropLastWhile(String::isEmpty)
                if (mentionList.size != 2) {
                    return false
                }
                return true
            }
        }
        return false
    }

    override fun onUserNicknameChanged(userKey: UserKey, nick: String?) {
        val user = liveUser.user ?: return
        if (user.key != userKey) return
        displayUser(user)
    }

    override fun onUserColorChanged(userKey: UserKey, color: Int) {
        val user = liveUser.user ?: return
        if (user.key != userKey) return
        displayUser(user)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (profileBirthdayStub == null && profileBirthdayBanner.visibility == View.VISIBLE) {
            return profileBirthdayBanner.dispatchTouchEvent(event)
        }
        return profileBanner.dispatchTouchEvent(event)
    }

    override fun scrollToStart(): Boolean {
        val fragment = currentVisibleFragment as? RefreshScrollTopInterface ?: return false
        fragment.scrollToStart()
        return true
    }

    override fun triggerRefresh(): Boolean {
        val fragment = currentVisibleFragment as? RefreshScrollTopInterface ?: return false
        fragment.triggerRefresh()
        return true
    }

    override fun onRefreshComplete(fragment: AbsContentRecyclerViewFragment<*, *>) {
    }

    private fun getFriendship() {
        val user = liveUser.user ?: return
        liveRelationship.user = user
        liveRelationship.load()
        if (user.isSelf) {
            followContainer.follow.visibility = View.VISIBLE
        } else {
            followContainer.follow.visibility = View.GONE
        }
        followingYouIndicator.visibility = View.GONE
        followProgress.visibility = View.VISIBLE
    }

    private fun getUserInfo(omitIntentExtra: Boolean) {
        if (liveUser.user != null || omitIntentExtra) {
            liveUser.extraUser = null
        } else {
            liveUser.extraUser = arguments?.user
        }
        liveUser.load()
        if (liveUser.user == null) {
            errorContainer.visibility = View.GONE
            progressContainer.visibility = View.VISIBLE
            errorText.text = null
            errorText.visibility = View.GONE
            setContentVisible(false)
        }
    }

    private fun setUiColor(@ColorInt color: Int) {
        val activity = activity as? BaseActivity ?: return
        val theme = Chameleon.getOverrideTheme(activity, activity)
        val actualColor = if (color != 0) color else theme.colorPrimary
        uiColor = actualColor
        primaryColor = if (theme.isToolbarColored) {
            actualColor
        } else {
            theme.colorToolbar
        }
        (toolbar.background as? ActionBarDrawable)?.color = primaryColor
        val taskColor = if (theme.isToolbarColored) {
            ColorUtils.setAlphaComponent(actualColor, 0xFF)
        } else {
            ColorUtils.setAlphaComponent(theme.colorToolbar, 0xFF)
        }
        val user = this.liveUser.user
        if (user != null) {
            val name = userColorNameManager.getDisplayName(user, nameFirst)
            ActivitySupport.setTaskDescription(activity, TaskDescriptionCompat(name, null, taskColor))
        } else {
            ActivitySupport.setTaskDescription(activity, TaskDescriptionCompat(null, null, taskColor))
        }

        // This call make sure status bar color changed
        coordinatorLayout.dispatchDependentViewsChanged(profileHeader)

        val optimalAccentColor = ThemeUtils.getOptimalAccentColor(actualColor, description.currentTextColor)
        description.setLinkTextColor(optimalAccentColor)
        location.setLinkTextColor(optimalAccentColor)
        url.setLinkTextColor(optimalAccentColor)
        profileBanner.setBackgroundColor(actualColor)
    }

    private fun setupViewStyle() {
        val activity = activity as? LinkHandlerActivity ?: return
        val actionBar = activity.supportActionBar ?: return
        val theme = Chameleon.getOverrideTheme(activity, activity)
        if (!ThemeUtils.isWindowFloating(activity) && theme is UserTheme) {
            profileBanner.alpha = theme.backgroundAlpha / 255f
        }

        actionBar.setBackgroundDrawable(ActionBarDrawable(ResourcesCompat.getDrawable(activity.resources,
                R.drawable.shadow_user_banner_action_bar, null)!!))

        val actionBarElevation = ThemeUtils.getSupportActionBarElevation(activity)
        ViewCompat.setElevation(toolbar, actionBarElevation)
        ViewCompat.setElevation(statusBarBackground, actionBarElevation * 2)

        ViewSupport.setOutlineProvider(toolbar, ViewOutlineProviderCompat.BACKGROUND)
        ViewSupport.setOutlineProvider(statusBarBackground, null)
    }


    private fun setupViewSettings() {
        profileImage.style = preferences[profileImageStyleKey]

        val lightFont = preferences[lightFontKey]

        nameContainer.name.applyFontFamily(lightFont)
        nameContainer.screenName.applyFontFamily(lightFont)
        nameContainer.followingYouIndicator.applyFontFamily(lightFont)
        description.applyFontFamily(lightFont)
        url.applyFontFamily(lightFont)
        location.applyFontFamily(lightFont)
        createdAt.applyFontFamily(lightFont)
    }

    private fun setupUserPages() {
        val args = arguments!!
        val user = args.user
        fun hasFavoriteTab(): Boolean {
            val accountKey = user?.account_key ?: args.accountKey
            return liveUser.account?.type != AccountType.MASTODON || liveUser.account?.key == accountKey
        }

        val tabArgs = Bundle {
            if (user != null) {
                this.accountKey = user.account_key
                this.userKey = user.key
                this.screenName = user.screen_name
                this.profileUrl = user.extras?.statusnet_profile_url
            } else {
                this.accountKey = args.accountKey
                this.userKey = args.userKey
                this.screenName = args.screenName
                this.profileUrl = args.profileUrl
            }
        }

        pagerAdapter.add(cls = UserTimelineFragment::class.java, args = Bundle(tabArgs) {
            this[UserTimelineFragment.EXTRA_ENABLE_TIMELINE_FILTER] = true
            this[UserTimelineFragment.EXTRA_LOAD_PINNED_STATUS] = true
        }, name = getString(R.string.title_statuses), type = TAB_TYPE_STATUSES, position = TAB_POSITION_STATUSES)
        pagerAdapter.add(cls = UserMediaTimelineFragment::class.java, args = Bundle(tabArgs) {
            this[EXTRA_TIMELINE_STYLE] = TimelineStyle.STAGGERED
        }, name = getString(R.string.media), type = TAB_TYPE_MEDIA, position = TAB_POSITION_MEDIA)

        if (hasFavoriteTab()) {
            if (preferences[iWantMyStarsBackKey]) {
                pagerAdapter.add(cls = FavoritesTimelineFragment::class.java, args = tabArgs,
                        name = getString(R.string.title_favorites), type = TAB_TYPE_FAVORITES,
                        position = TAB_POSITION_FAVORITES)
            } else {
                pagerAdapter.add(cls = FavoritesTimelineFragment::class.java, args = tabArgs,
                        name = getString(R.string.title_likes), type = TAB_TYPE_FAVORITES,
                        position = TAB_POSITION_FAVORITES)
            }
        }
    }

    private fun ParcelableRelationship.check(user: ParcelableUser): Boolean {
        if (account_key != user.account_key) {
            return false
        }
        return user_key.id == user.extras?.unique_id || user_key.id == user.key.id
    }

    private fun setFollowEditButton(@DrawableRes icon: Int, @ColorRes color: Int, @StringRes label: Int) {
        val followButton = followContainer.follow
        followButton.setImageResource(icon)
        ViewCompat.setBackgroundTintMode(followButton, PorterDuff.Mode.SRC_ATOP)
        val backgroundTintList = ContextCompat.getColorStateList(context!!, color)!!
        ViewCompat.setBackgroundTintList(followButton, backgroundTintList)
        ImageViewCompat.setImageTintList(followButton,
                ColorStateList.valueOf(ThemeUtils.getContrastColor(backgroundTintList.defaultColor,
                        Color.BLACK, Color.WHITE)))
        followButton.contentDescription = getString(label)
    }

    private fun updateOptionsMenuVisibility() {
        setHasOptionsMenu(liveUser.user != null && liveRelationship.relationship != null)
    }

    @UiThread
    private fun displayUser(user: ParcelableUser) {
        val activity = activity ?: return
        val adapter = pagerAdapter
        (0 until adapter.count).forEach { i ->
            val sf = adapter.instantiateItem(viewPager, i) as? AbsTimelineFragment ?: return@forEach
            if (sf.view == null) return@forEach
        }
        errorContainer.visibility = View.GONE
        progressContainer.visibility = View.GONE
        setContentVisible(true)
        profileImage.setBorderColor(if (user.color != 0) user.color else Color.WHITE)
        followContainer.drawEnd(user.account_color)
        nameContainer.name.setText(bidiFormatter.unicodeWrap(when {
            user.nickname.isNullOrEmpty() -> user.name
            else -> getString(R.string.name_with_nickname, user.name, user.nickname)
        }), TextView.BufferType.SPANNABLE)
        val typeIconRes = Utils.getUserTypeIconRes(user.is_verified, user.is_protected)
        if (typeIconRes != 0) {
            profileType.setImageResource(typeIconRes)
            profileType.visibility = View.VISIBLE
        } else {
            profileType.setImageDrawable(null)
            profileType.visibility = View.GONE
        }
        @SuppressLint("SetTextI18n")
        nameContainer.screenName.spannable = "@${user.acct}"
        val linkHighlightOption = preferences[linkHighlightOptionKey]
        val linkify = TwidereLinkify(this, linkHighlightOption)

        if (user.description_unescaped != null) {
            val text = SpannableStringBuilder.valueOf(user.description_unescaped).apply {
                user.description_spans?.applyTo(this, null, requestManager, description)
                linkify.applyAllLinks(this, user.account_key, false, false)
            }
            description.spannable = text
        } else {
            description.spannable = user.description_plain
            Linkify.addLinks(description, Linkify.WEB_URLS)
        }
        description.hideIfEmpty()

        location.spannable = user.location
        location.hideIfEmpty()

        url.spannable = user.urlPreferred?.let {
            val ssb = SpannableStringBuilder(it)
            ssb.setSpan(TwidereURLSpan(it, highlightStyle = linkHighlightOption), 0, ssb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return@let ssb
        }
        this.url.hideIfEmpty()

        if (user.created_at >= 0) {
            val createdAt = Utils.formatToLongTimeString(activity, user.created_at)
            val daysSinceCreation = (System.currentTimeMillis() - user.created_at) / 1000 / 60 / 60 / 24.toFloat()
            val dailyTweets = Math.round(user.statuses_count / Math.max(1f, daysSinceCreation))
            this.createdAt.text = resources.getQuantityString(R.plurals.created_at_with_N_tweets_per_day, dailyTweets,
                    createdAt, dailyTweets)
        } else {
            this.createdAt.text = null
        }
        this.createdAt.hideIfEmpty()

        val locale = Locale.getDefault()

        listedCount.primaryText = Utils.getLocalizedNumber(locale, user.listed_count)
        groupsCount.primaryText = Utils.getLocalizedNumber(locale, user.groupsCount)
        followersCount.primaryText = Utils.getLocalizedNumber(locale, user.followers_count)
        friendsCount.primaryText = Utils.getLocalizedNumber(locale, user.friends_count)

        listedCount.updateText()
        groupsCount.updateText()
        followersCount.updateText()
        friendsCount.updateText()

        listedCount.visibility = if (user.listed_count < 0) View.GONE else View.VISIBLE
        groupsCount.visibility = if (user.groupsCount < 0) View.GONE else View.VISIBLE

        setUiColor(when {
            user.color != 0 -> user.color
            user.link_color != 0 -> user.link_color
            else -> Chameleon.getOverrideTheme(activity, activity).colorPrimary
        })
        val defWidth = resources.displayMetrics.widthPixels
        requestManager.loadProfileBanner(activity, user, defWidth).into(profileBanner)
        requestManager.loadOriginalProfileImage(activity, user, profileImage.style,
                profileImage.cornerRadius, profileImage.cornerRadiusRatio)
                .thumbnail(requestManager.loadProfileImage(activity, user, profileImage.style,
                        profileImage.cornerRadius, profileImage.cornerRadiusRatio,
                        getString(R.string.profile_image_size))).into(profileImage)
        activity.title = SpannableStringBuilder.valueOf(UserColorNameManager.decideDisplayName(user.nickname, user.name,
                user.screen_name, nameFirst)).also {
            externalThemeManager.emoji?.applyTo(it)
        }

        val userCreationDay = condition@ if (user.created_at >= 0) {
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            cal.timeInMillis = user.created_at
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.DAY_OF_MONTH) == currentDay
        } else {
            false
        }

        if (userCreationDay && !hideBirthdayView) {
            if (profileBirthdayStub != null) {
                profileBirthdayBanner = profileBirthdayStub.inflate()
                profileBirthdayBanner.setOnClickListener(this)
            } else {
                profileBirthdayBanner.visibility = View.VISIBLE
            }
        } else if (profileBirthdayStub == null) {
            profileBirthdayBanner.visibility = View.GONE
        }

        url.movementMethod = null

        activity.invalidateOptionsMenu()
        updateSubtitle()
    }

    private fun setContentVisible(visible: Boolean) {
        viewPagerContainer.setVisible(visible)
        profileBannerContainer.setVisible(visible)
        profileHeader.setVisible(visible)
        tabsShadow.setVisible(visible)
    }

    private fun displayRelationship(relationship: ParcelableRelationship) {
        val activity = activity ?: return
        val user = this.liveUser.user ?: return
        if (user.isSelf) {
            setFollowEditButton(R.drawable.ic_action_edit, R.color.material_light_blue,
                    R.string.action_edit)
            followContainer.follow.visibility = View.VISIBLE
            return
        }
        if (!relationship.check(user)) {
            return
        }
        activity.invalidateOptionsMenu()
        when {
            relationship.blocked_by -> {
                pagesErrorContainer.visibility = View.GONE
                pagesErrorText.text = null
            }
            !relationship.following && user.hide_protected_contents -> {
                pagesErrorContainer.visibility = View.VISIBLE
                pagesErrorText.setText(R.string.user_protected_summary)
                pagesErrorIcon.setImageResource(R.drawable.ic_info_locked)
            }
            else -> {
                pagesErrorContainer.visibility = View.GONE
                pagesErrorText.text = null
            }
        }
        when {
            relationship.blocking -> setFollowEditButton(R.drawable.ic_action_block, R.color.material_red,
                    R.string.action_unblock)
            relationship.blocked_by -> setFollowEditButton(R.drawable.ic_action_block, R.color.material_grey,
                    R.string.action_block)
            relationship.following -> setFollowEditButton(R.drawable.ic_action_confirm, R.color.material_light_blue,
                    R.string.action_unfollow)
            user.is_follow_request_sent -> setFollowEditButton(R.drawable.ic_action_time, R.color.material_light_blue,
                    R.string.label_follow_request_sent)
            else -> setFollowEditButton(R.drawable.ic_action_add, android.R.color.white,
                    R.string.action_follow)
        }
        followingYouIndicator.visibility = if (relationship.followed_by) View.VISIBLE else View.GONE

        val weakThis by weak(this)
        task {
            val resolver = weakThis?.context?.contentResolver ?: throw InterruptedException()
            resolver.insert(CachedUsers.CONTENT_URI, user, ParcelableUser::class.java)
            resolver.insert(CachedRelationships.CONTENT_URI, relationship, ParcelableRelationship::class.java)
        }
        followContainer.follow.visibility = View.VISIBLE
    }


    private fun showAddToListDialog(user: ParcelableUser) {
        val accountKey = user.account_key ?: return
        val weakThis by weak(this)
        executeAfterFragmentResumed {
            ProgressDialogFragment.show(it.childFragmentManager, "get_list_progress")
        }.then {
            val context = weakThis?.context ?: throw InterruptedException()
            fun MicroBlog.getUserListOwnerMemberships(id: String): ArrayList<UserList> {
                val result = ArrayList<UserList>()
                var nextCursor: Long
                val paging = Paging()
                paging.count(100)
                do {
                    val resp = getUserListMemberships(id, paging, true)
                    result.addAll(resp)
                    nextCursor = resp.nextCursor
                    paging.cursor(nextCursor)
                } while (nextCursor > 0)

                return result
            }

            val microBlog = AccountManager.get(context).getDetailsOrThrow(accountKey, true)
                    .newMicroBlogInstance(context, MicroBlog::class.java)
            val ownedLists = ArrayList<ParcelableUserList>()
            val listMemberships = microBlog.getUserListOwnerMemberships(user.key.id)
            val paging = Paging()
            paging.count(100)
            var nextCursor: Long
            do {
                val resp = microBlog.getUserListOwnerships(paging)
                resp.mapTo(ownedLists) { item ->
                    val userList = item.toParcelable(accountKey)
                    userList.is_user_inside = listMemberships.any { it.id == item.id }
                    return@mapTo userList
                }
                nextCursor = resp.nextCursor
                paging.cursor(nextCursor)
            } while (nextCursor > 0)
            return@then ownedLists.toTypedArray()
        }.alwaysUi {
            val fragment = weakThis ?: return@alwaysUi
            fragment.executeAfterFragmentResumed {
                it.childFragmentManager.dismissDialogFragment("get_list_progress")
            }
        }.successUi { result ->
            val fragment = weakThis ?: return@successUi
            fragment.executeAfterFragmentResumed { f ->
                val df = AddRemoveUserListDialogFragment()
                df.arguments = Bundle {
                    this[EXTRA_ACCOUNT_KEY] = accountKey
                    this[EXTRA_USER_KEY] = user.key
                    this[EXTRA_USER_LISTS] = result
                }
                df.show(f.childFragmentManager, "add_remove_list")
            }
        }.failUi {
            val context = weakThis?.context ?: return@failUi
            Toast.makeText(context, it.getErrorMessage(context), Toast.LENGTH_SHORT).show()
        }
    }

    class AddRemoveUserListDialogFragment : BaseDialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val args = arguments!!
            val lists = args.getTypedArray<ParcelableUserList>(EXTRA_USER_LISTS)
            val userKey = args.getParcelable<UserKey>(EXTRA_USER_KEY)
            val accountKey = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle(R.string.title_add_or_remove_from_list)
            val entries = Array(lists.size) { idx ->
                lists[idx].name
            }
            val states = BooleanArray(lists.size) { idx ->
                lists[idx].is_user_inside
            }
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setNeutralButton(R.string.new_user_list, null)
            builder.setNegativeButton(android.R.string.cancel, null)

            builder.setMultiChoiceItems(entries, states, null)
            val dialog = builder.create()
            dialog.onShow { d ->
                d.applyTheme()
                d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val checkedPositions = d.listView.checkedItemPositions
                    val weakActivity by weak(activity)
                    (activity as IBaseActivity<*>).executeAfterFragmentResumed {
                        ProgressDialogFragment.show(it.supportFragmentManager, "update_lists_progress")
                    }.then {
                        val activity = weakActivity ?: throw IllegalStateException()
                        val twitter = AccountManager.get(activity).getDetailsOrThrow(accountKey, true)
                                .newMicroBlogInstance(activity, MicroBlog::class.java)
                        val successfulStates = SparseBooleanArray()
                        try {
                            for (i in 0 until checkedPositions.size()) {
                                val pos = checkedPositions.keyAt(i)
                                val checked = checkedPositions.valueAt(i)
                                if (states[pos] != checked) {
                                    if (checked) {
                                        twitter.addUserListMember(lists[pos].id, userKey.id)
                                    } else {
                                        twitter.deleteUserListMember(lists[pos].id, userKey.id)
                                    }
                                    successfulStates.put(pos, checked)
                                }
                            }
                        } catch (e: MicroBlogException) {
                            throw UpdateListsException(e, successfulStates)
                        }
                    }.alwaysUi {
                        val activity = weakActivity as? IBaseActivity<*> ?: return@alwaysUi
                        activity.executeAfterFragmentResumed { a ->
                            val manager = a.supportFragmentManager
                            val df = manager.findFragmentByTag("update_lists_progress") as? DialogFragment
                            df?.dismiss()
                        }
                    }.successUi {
                        dismiss()
                    }.failUi { e ->
                        val activity = weakActivity ?: return@failUi
                        if (e is UpdateListsException) {
                            val successfulStates = e.successfulStates
                            for (i in 0 until successfulStates.size()) {
                                val pos = successfulStates.keyAt(i)
                                val checked = successfulStates.valueAt(i)
                                d.listView.setItemChecked(pos, checked)
                                states[pos] = checked
                            }
                        }
                        Toast.makeText(activity, e.getErrorMessage(activity), Toast.LENGTH_SHORT).show()
                    }
                }
                d.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                    val df = CreateUserListDialogFragment()
                    df.arguments = Bundle {
                        this[EXTRA_ACCOUNT_KEY] = accountKey
                    }
                    df.show(fragmentManager, "create_user_list")
                }
            }
            return dialog
        }

        class UpdateListsException(cause: Throwable, val successfulStates: SparseBooleanArray) : MicroBlogException(cause)
    }

    companion object {

        private const val TAB_POSITION_STATUSES = 0
        private const val TAB_POSITION_MEDIA = 1
        private const val TAB_POSITION_FAVORITES = 2
        private const val TAB_TYPE_STATUSES = "statuses"
        private const val TAB_TYPE_STATUSES_WITH_REPLIES = "statuses_with_replies"
        private const val TAB_TYPE_MEDIA = "media"
        private const val TAB_TYPE_FAVORITES = "favorites"

        private val ParcelableUser.hide_protected_contents: Boolean
            get() = user_type != AccountType.MASTODON && is_protected
    }
}
