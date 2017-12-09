/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.twidere.view.behavior.userprofile

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.header_user.view.*
import org.mariotaku.twidere.R
import org.mariotaku.twidere.graphic.drawable.userprofile.ActionBarDrawable
import org.mariotaku.twidere.util.ThemeUtils

internal class ToolbarBehavior(context: Context?, attrs: AttributeSet? = null) : CoordinatorLayout.Behavior<Toolbar>(context, attrs) {

    private val actionBarShadowColor: Int = 0xA0000000.toInt()
    private var actionItemIsDark: Int = 0

    private val argbEvaluator = ArgbEvaluator()

    override fun layoutDependsOn(parent: CoordinatorLayout, child: Toolbar, dependency: View): Boolean {
        return dependency.id == R.id.profileHeader
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: Toolbar, layoutDirection: Int): Boolean {
        val ret = super.onLayoutChild(parent, child, layoutDirection)
        updateToolbarFactor(parent, child, parent.getDependencies(child).first())
        return ret
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: Toolbar, dependency: View): Boolean {
        updateToolbarFactor(parent, child, dependency)
        return false
    }

    @SuppressLint("RestrictedApi")
    private fun updateToolbarFactor(parent: CoordinatorLayout, child: Toolbar, dependency: View): Boolean {
        val actionBarBackground = child.background as? ActionBarDrawable ?: return true
        val bannerContainer = parent.profileBannerContainer
        val detailsBackground = parent.profileHeaderBackground
        val detailsBottom = dependency.top + detailsBackground.bottom
        val headerHidden = dependency.visibility == View.GONE

        val colorFactor = colorFactor(dependency, bannerContainer, child)

        val outlineFactor = when {
            headerHidden -> 1f
            colorFactor < 1 -> colorFactor
            else -> ((detailsBottom - child.bottom) / detailsBackground.height.toFloat()).coerceIn(0f, 1f)
        }

        actionBarBackground.factor = colorFactor
        actionBarBackground.outlineAlphaFactor = outlineFactor

        val colorPrimary = actionBarBackground.color
        val currentActionBarColor = argbEvaluator.evaluate(colorFactor, actionBarShadowColor,
                colorPrimary) as Int
        val actionItemIsDark = if (ThemeUtils.isLightColor(currentActionBarColor)) 1 else -1
        if (this.actionItemIsDark != actionItemIsDark) {
            ThemeUtils.applyToolbarItemColor(parent.context, child, currentActionBarColor)
        }
        this.actionItemIsDark = actionItemIsDark
        return false
    }

    companion object {

        fun colorFactor(header: View, banner: View, toolbar: View): Float {
            val bannerTop = header.top
            val bannerHeight = banner.measuredHeight
            val colorFactorOffsetMin = toolbar.bottom - bannerHeight

            val headerHidden = header.visibility == View.GONE

            return when {
                headerHidden -> 1f
                else -> (bannerTop.toFloat() / colorFactorOffsetMin).coerceIn(0f, 1f)
            }
        }
    }
}
