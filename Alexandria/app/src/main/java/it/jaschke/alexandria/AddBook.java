package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import it.jaschke.alexandria.CameraPreview.BarcodeScannerActivity;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_SCAN = 0;
    private static final int LOADER_ID = 1;

    private final String EAN_CONTENT="eanContent";
    @InjectView(R.id.etISBN) EditText mEtISBN;
    @InjectView(R.id.tvBookTitle) TextView mTvBookTitle;
    @InjectView(R.id.tvBookSubtitle) TextView mTvBookSubTitle;
    @InjectView(R.id.tvAuthors) TextView mTvAuthors;
    @InjectView(R.id.tvCategories) TextView mTvCategories;
    @InjectView(R.id.ivBookCover) ImageView mIvBookCover;
    @InjectView(R.id.btnSave) View mBtnSave;
    @InjectView(R.id.btnDelete) View mBtnDelete;
    @Optional @InjectView(R.id.btnScan) View mBtnScan;
    private Toast mCurrentToast;


    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mEtISBN !=null) {
            outState.putString(EAN_CONTENT, mEtISBN.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ButterKnife.inject(this, rootView);

        mEtISBN.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                if (ean.length() < 13) {
                    clearFields();
                    return;
                }
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();
            }
        });

        rootView.findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentToast != null){
                    mCurrentToast.cancel();
                    mCurrentToast = null;
                }
                updateBook(mEtISBN.getText().toString());
                mCurrentToast = Toast.makeText(getActivity(),getString(R.string.book_added),Toast.LENGTH_LONG);
                mCurrentToast.show();
                mBtnSave.setEnabled(false);
                mBtnDelete.setEnabled(true);
            }
        });

        rootView.findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteBook(mEtISBN.getText().toString());
                mEtISBN.setText("");
            }
        });

        if(savedInstanceState!=null){
            mEtISBN.setText(savedInstanceState.getString(EAN_CONTENT));
            mEtISBN.setHint("");
        }
        if (mBtnScan!=null){
            mBtnScan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(getActivity(), BarcodeScannerActivity.class), REQUEST_SCAN);
                }
            });
        }

        return rootView;
    }

    private void deleteBook(String currentBookISBN) {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, currentBookISBN);
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);
    }

    private void updateBook(String currentBookISBN) {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, currentBookISBN);
        bookIntent.putExtra(BookService.EXTRA_SHOWN_IN_LIST, true);
        bookIntent.setAction(BookService.UPDATE_BOOK);
        getActivity().startService(bookIntent);
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(mEtISBN.getText().length()==0){
            return null;
        }
        String eanStr= mEtISBN.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }
        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        mTvBookTitle.setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        mTvBookSubTitle.setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        mTvAuthors.setLines(authorsArr.length);
        mTvAuthors.setText(authors.replace(",", "\n"));
        mTvAuthors.setText(authors.replace(",", "\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage(mIvBookCover).execute(imgUrl);
            mIvBookCover.setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        mTvCategories.setText(categories);
        mBtnDelete.setEnabled(false);
        mBtnScan.setEnabled(true);
        mBtnSave.setVisibility(View.VISIBLE);
        mBtnDelete.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        mTvBookTitle.setText("");
        mTvBookSubTitle.setText("");
        mTvAuthors.setText("");
        mTvCategories.setText("");
        mIvBookCover.setVisibility(View.INVISIBLE);
        mBtnSave.setVisibility(View.INVISIBLE);
        mBtnDelete.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN){
            if (resultCode == Activity.RESULT_OK){
                mEtISBN.setText(data.getStringExtra(BarcodeScannerActivity.SCANNER_EXTRA));
            }
        }
    }
}
