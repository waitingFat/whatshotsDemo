package com.whatsmode.shopify.block.cart;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.shopify.graphql.support.ID;
import com.whatsmode.library.util.ListUtils;
import com.whatsmode.library.util.PreferencesUtil;
import com.whatsmode.shopify.R;
import com.whatsmode.shopify.WhatsApplication;
import com.whatsmode.shopify.base.BaseRxPresenter;
import com.whatsmode.shopify.common.Constant;
import com.whatsmode.shopify.ui.helper.CommonAdapter;
import com.whatsmode.shopify.ui.helper.CommonViewHolder;
import com.zchu.log.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

class CartPresenter extends BaseRxPresenter<CartContact.View> implements CartContact.Presenter {

    @Override
    public void doLoadData(boolean isRefresh) {
//        if (isRefresh && mAdapter != null) {
//            saveCart(mAdapter.getData());
//        }
        Observable.create((ObservableOnSubscribe<List<CartItem>>) e -> {
            try {
                List<CartItem> cartItemList = (List<CartItem>) PreferencesUtil.getObject(WhatsApplication.getContext(), Constant.CART_LOCAL);
                if (ListUtils.isEmpty(cartItemList)) {
                    cartItemList = new ArrayList<>();
                }
                e.onNext(cartItemList);
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems -> {
                    if (isViewAttached()) {
                        getView().setAdapter(createAdapter(cartItems));
                        getView().clearCheckItems(selectAll);
                        if (!ListUtils.isEmpty(cartItems)) {
                            getView().showContent(true);
                        } else {
                            getView().showTheEnd();
                        }
                        getView().checkSpanner();

                    }
                });
    }

    private CommonAdapter<CartItem> mAdapter;

    private BaseQuickAdapter createAdapter(List<CartItem> cartItems) {
        mAdapter = new CommonAdapter<CartItem>(R.layout.item_cart, cartItems) {
            @Override
            protected void convert(CommonViewHolder helper, CartItem item) {
                ImageView ivIcon  = helper.getView(R.id.icon);
                View view = helper.getView(R.id.aboveView);
                view.setVisibility(item.isSoldOut?View.VISIBLE:View.GONE);
                LinearLayout operationLayout = helper.getView(R.id.operation_layout);
                if (getView().isCurrentDelete() || item.isSoldOut) {
                    operationLayout.setVisibility(View.GONE);
                } else {
                    operationLayout.setVisibility(View.VISIBLE);
                }
                ImageView soldOut = helper.getView(R.id.icon_sold_out);
                soldOut.setVisibility(item.isSoldOut ? View.VISIBLE : View.GONE);
                View line = helper.getView(R.id.separator);
                View reduceView = helper.getView(R.id.reduce_view);
                TextView addView = helper.getView(R.id.add_view);
                line.setVisibility(helper.getAdapterPosition() == cartItems.size() -1 ? View.GONE:View.VISIBLE);
                TextView tvQuality = helper.getView(R.id.quality);
                LinearLayout llReduce = helper.getView(R.id.reduce);
                LinearLayout llAdd = helper.getView(R.id.add);
                Glide.with(WhatsApplication.getContext())
                        .load(item.getIcon())
                        .asBitmap()
                        .centerCrop()
                        .placeholder(R.drawable.defaut_product)
                        .error(R.drawable.defaut_product)
                        .into(ivIcon);
                helper.setText(R.id.description, item.name)
                        .setText(R.id.sizeAndColor,item.getColorAndSize())
                        .setText(R.id.price, new StringBuilder("$").append(String.valueOf(item.getPrice())))
                        .setText(R.id.quality, String.valueOf(item.quality));
                TextView comparePrice = helper.getView(R.id.comparePrice);
                if (Double.doubleToLongBits(item.getPrice()) == Double.doubleToLongBits(item.getComparePrice())) {
                    comparePrice.setVisibility(View.GONE);
                } else {
                    comparePrice.setVisibility(View.VISIBLE);
                    comparePrice.setText(new StringBuilder("$").append(String.valueOf(item.getComparePrice())).append("USD"));
                    comparePrice.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                }

                if (Integer.parseInt(tvQuality.getText().toString()) == 1) {
                    reduceView.setBackgroundColor(Color.parseColor("#bbbbbb"));
                }else{
                    reduceView.setBackgroundColor(Color.BLACK);
                }

                if (Integer.parseInt(tvQuality.getText().toString()) == 99) {
                    llAdd.setClickable(false);
                    addView.setTextColor(Color.parseColor("#bbbbbb"));
                }else{
                    llAdd.setClickable(true);
                    addView.setTextColor(Color.BLACK);
                }
                llReduce.setOnClickListener(v -> {
                    item.quality = Math.max(1, Integer.parseInt(tvQuality.getText().toString())- 1);
                    if (item.quality == 1) {
                        reduceView.setBackgroundColor(Color.parseColor("#bbbbbb"));
                        addView.setTextColor(Color.BLACK);
                    }else{
                        reduceView.setBackgroundColor(Color.BLACK);
                        if (item.quality == 99) {
                            addView.setTextColor(Color.parseColor("#bbbbbb"));
                        }else{
                            addView.setTextColor(Color.BLACK);
                        }
                    }
                    tvQuality.setText(String.valueOf(item.quality));
                    if (isViewAttached()) {
                        getView().checkTotal();
                    }
                });
                View ivCheck = helper.getView(R.id.iv_radio);
                if (isViewAttached()) {
                    if (item.isSoldOut) {
                        ivCheck.setEnabled(getView().isCurrentDelete()?true:false);
                    }else{
                        ivCheck.setVisibility(View.VISIBLE);
                    }
                }
                ivCheck.setSelected(selectAll);
                if (getView().getCheckedCartItem().contains(item)) {
                    ivCheck.setSelected(true);
                }else{
                    ivCheck.setSelected(false);
                }
                if (item.isSoldOut && !getView().isCurrentDelete()) {
                    ivCheck.setSelected(false);
                }
                ivCheck.setOnClickListener(v -> {
                    if (isViewAttached()) {
                        ivCheck.setSelected(!ivCheck.isSelected());
                        getView().onCheckSelect(ivCheck.isSelected(), item);
                    }
                });

                llAdd.setOnClickListener(v -> {
                    int quality = Integer.parseInt(tvQuality.getText().toString()) + 1;
                    quality = Math.min(quality, 99);
                    item.quality = quality;
                    if (item.quality == 99) {
                        addView.setTextColor(Color.parseColor("#bbbbbb"));
                        reduceView.setBackgroundColor(Color.BLACK);
                    }else{
                        if (item.quality == 1) {
                            reduceView.setBackgroundColor(Color.parseColor("#bbbbbb"));
                        }else{
                            reduceView.setBackgroundColor(Color.BLACK);
                        }
                        addView.setTextColor(Color.BLACK);
                    }
                    tvQuality.setText(String.valueOf(quality));
                    if (isViewAttached()) {
                        getView().checkTotal();
                    }
                });
                helper.itemView.setOnClickListener(v -> {
                    if (!item.isSoldOut)
                    getView().jumpToDetail(item);
                });
                helper.itemView.setOnLongClickListener(v -> {
                    if (isViewAttached()) {
                        getView().deleteItem(item);
                    }
                    return true;
                });
            }
        };
        return mAdapter;
    }

    @Override
    public void doLoadMoreData() {
        io.reactivex.Observable.create((ObservableOnSubscribe<List<CartItem>>)
                e -> e.onNext(CartItem.mockItem()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems1 -> {
                    if (mAdapter != null && isViewAttached()) {
                        getView().showTheEnd();
                    }
                });
    }

    @Override
    public void saveCart(List<CartItem> data) {
        try {
            PreferencesUtil.putObject(WhatsApplication.getContext(), Constant.CART_LOCAL, data);
        } catch (IOException e) {
            Logger.e(e);
            e.printStackTrace();
        }
    }

    public boolean selectAll = true;

    @Override
    public void onClickView(View v) {
        switch (v.getId()) {
            case R.id.checkOut:
                if (isViewAttached()) {
                    checkOut(getView().getCheckedCartItem());
                }
                break;
            case R.id.checkOut_select:
                if (isViewAttached()) {
                    if (mAdapter != null) {
                        selectAll = !selectAll;
                        getView().selectAll(selectAll);
                        mAdapter.notifyDataSetChanged();
                    }
                    v.setSelected(!v.isSelected());
                }
                break;
            case R.id.delete:
                getView().showDeleteDialog();
                break;
        }
    }

    @Override
    public boolean isSelectAll() {
        return selectAll;
    }

    @Override
    public void setSelectAll(boolean isSelectAll,boolean isNotify) {
        selectAll = isSelectAll;
        if(isNotify)
        mAdapter.notifyDataSetChanged();
    }

    private void checkOut(List<CartItem> data) {
        if (!ListUtils.isEmpty(data)) {
            if (isViewAttached()) {
                getView().showLoading(false);
            }
            CartRepository.create().parameter(data).checkoutListener(new CartRepository.QueryListener() {
                @Override
                public void onSuccess(Double price,ID id,List<CartItem> response) {
                    if (isViewAttached()) {
                        getView().hideLoading();
                        getView().showSuccess(price,id,response);
                    }
                }

                @Override
                public void onError(String message) {
                    if (isViewAttached()) {
                        getView().hideLoading();
                        getView().showError(message);
                    }
                }
            }).execute();
        } else {
            if(isViewAttached())
            getView().showError(WhatsApplication.getContext().getString(R.string.plz_select_products));
        }
    }
}
