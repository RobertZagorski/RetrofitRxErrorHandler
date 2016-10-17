package com.rzagorski.retrofitrxerrorhandler.backoff.strategies;

import java.util.List;

public interface AddThrowable<T> {

    /**
     * When this flag is enabled the retry is made for everything except the parameters set
     * in this backoff strategy.
     *
     * @return the Builder to add more parameters
     */
    public T exclusive();

    /**
     * Adds a single {@link java.lang.Throwable} to the list of checked errors.
     *
     * @param throwableForBackoff the class of Error or Exception that extends {@link java.lang.Throwable}
     * @return the Builder to add more parameters
     */
    public T addThrowable(Class<? extends Throwable> throwableForBackoff);

    /**
     * Adds a list of {@link java.lang.Throwable}.
     * <br>
     * A list of previously added errors will be overriden.
     *
     * @param throwableForBackoffList the list of {@link java.lang.Throwable}
     * @return the Builder to add more parameters
     */
    public T setThrowable(List<Class<? extends Throwable>> throwableForBackoffList);

    /**
     * Adds a list of HTTP code.
     * <br>
     * A list of previously added codes will be overriden.
     *
     * @param codes the list of code of type {@link java.lang.Integer}.
     * @return the Builder to add more parameters.
     */
    public T setHttpCodeList(List<Integer> codes);

    /**
     * Adds a single code to the list of checked HTTP codes.
     *
     * @param code of type {@link java.lang.Integer Integer} a HTTP code to be checked taken from response.
     * @return the Builder to add more parameters
     */
    public T addHttpCode(int code);
}