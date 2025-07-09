import React, { useState, useCallback } from 'react';
import Button from '../../../components/ui/Button'; // Adjust path if necessary
import './PreferenceSurvey.css'; // We will update this CSS

// --- Option Definitions (remain the same) ---
const eventTypes = [
    { value: 'Koncert', label: 'Koncert/Muzyka' },
    { value: 'Teatr', label: 'Teatr' },
    { value: 'Sport', label: 'Sport' },
    { value: 'Warsztaty', label: 'Warsztaty/Edukacja' },
    { value: 'Wystawa', label: 'Wystawa/Sztuka' },
    { value: 'Festiwal', label: 'Festiwal' },
    { value: 'Targi', label: 'Targi/Biznes' },
    { value: 'Rozrywka', label: 'Rozrywka/Inne' }
];

const venueTypes = [
    { value: 'Mały klub/Bar', label: 'Mały klub / Bar' },
    { value: 'Duża sala/Hala', label: 'Duża sala / Hala' },
    { value: 'Teatr', label: 'Teatr' },
    { value: 'Stadion/Arena', label: 'Stadion / Arena' },
    { value: 'Plener', label: 'Plener' },
    { value: 'Muzeum/Galeria', label: 'Muzeum / Galeria' }
];

const timePreferences = [
    { value: 'Najbliższy weekend', label: 'Najbliższy weekend' },
    { value: 'Wieczory w tygodniu', label: 'Wieczory w tygodniu' },
    { value: 'Popołudnia w weekend', label: 'Popołudnia w weekend' },
    { value: 'W ciągu dnia (tydzień)', label: 'W ciągu dnia (tydzień)' },
    { value: 'Dowolny termin', label: 'Dowolny termin' }
];

const atmosphereOptions = [
    { value: 'Kameralna', label: 'Kameralna' },
    { value: 'Masowa/Festiwalowa', label: 'Masowa / Festiwalowa' },
    { value: 'Energiczna', label: 'Energiczna' },
    { value: 'Spokojna/Relaksująca', label: 'Spokojna / Relaksująca' },
    { value: 'Taneczna', label: 'Taneczna' },
    { value: 'Dla rodzin', label: 'Dla rodzin' },
    { value: 'Tylko dla dorosłych', label: 'Tylko dla dorosłych' }
];
// --- End Option Definitions ---

const surveySteps = [
    { id: 1, title: 'Typ Wydarzenia i Gatunek' },
    { id: 2, title: 'Lokalizacja i Miejsce' },
    { id: 3, title: 'Czas i Artyści' },
    { id: 4, title: 'Atmosfera i Dodatkowy Opis' },
];

/**
 * Multi-step component for collecting user preferences for event recommendations.
 * Features a modernized, more horizontal layout for a cleaner user experience.
 */
const PreferenceSurvey = ({ onSurveySubmit, totalEvents = 0, isLoading = false, error = null }) => {
    const [isSurveyActive, setIsSurveyActive] = useState(false);
    const [currentStep, setCurrentStep] = useState(1);
    const [formData, setFormData] = useState({
        eventTypes: [],
        genreTags: '',
        artistPref: 'Bez znaczenia',
        location: 'Warszawa',
        venueTypes: [],
        timePref: [],
        atmosphere: [],
        description: ''
    });

    // --- Form Input Handlers ---
    const handleInputChange = useCallback((e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    }, []);

    const handleRadioChange = useCallback((e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    }, []);

    const handleCheckboxChange = useCallback((e) => {
        const { name, value, checked } = e.target;
        setFormData(prev => {
            const currentValues = prev[name] || [];
            let updatedValues;
            if (checked) {
                if (name === 'venueTypes') {
                    if (value === 'Dowolne') { updatedValues = ['Dowolne']; }
                    else { updatedValues = currentValues.filter(item => item !== 'Dowolne'); updatedValues.push(value); }
                } else if (name === 'atmosphere') {
                    if (currentValues.length < 2) { updatedValues = [...currentValues, value]; }
                    else { return prev; }
                } else { updatedValues = [...currentValues, value]; }
            } else {
                if (name === 'venueTypes' && value === 'Dowolne') { updatedValues = []; }
                else { updatedValues = currentValues.filter(item => item !== value); }
            }
            return { ...prev, [name]: updatedValues };
        });
    }, []);

    // --- Step Navigation ---
    const nextStep = useCallback(() => {
        if (currentStep < surveySteps.length) {
            setCurrentStep(prev => prev + 1);
        }
    }, [currentStep, surveySteps.length]);

    const prevStep = useCallback(() => {
        if (currentStep > 1) {
            setCurrentStep(prev => prev - 1);
        }
    }, [currentStep]);

    // --- Explicit Submit Handler for the Final Button ---
    const handleFinalSubmit = useCallback(async () => {
        if (onSurveySubmit) {
            await onSurveySubmit(formData);
        }
    }, [onSurveySubmit, formData]);

    // --- Render Logic ---
    if (!isSurveyActive) {
        return (
            <div className="preference-survey-container initial-view">
                <div className="survey-text">
                    <h2>Wirtualny asystent pomoże Ci znaleźć idealne wydarzenie</h2>
                    <p>
                        Widzisz, ile mamy wydarzeń ({totalEvents || 'wiele'})? Trudno się zdecydować?
                        Nasz wirtualny asystent pomoże Ci wybrać coś dopasowanego do Twoich preferencji!
                    </p>
                </div>
                <Button onClick={() => setIsSurveyActive(true)} variant="primary" size="large" className="activate-assistant-btn">
                    ✨ Aktywuj Asystenta Wydarzeń ✨
                </Button>
            </div>
        );
    }

    return (
        <div className="preference-survey-container active-view">
            <div className="survey-header">
                <h2>{surveySteps[currentStep - 1].title}</h2>
                <div className="survey-progress">
                    <span>Krok {currentStep} z {surveySteps.length}</span>
                    <progress value={currentStep} max={surveySteps.length}></progress>
                </div>
            </div>

            {error && <p className="survey-error">{error}</p>}

            <form className="survey-form"> {/* No onSubmit here, handled by button click */}
                {currentStep === 1 && (
                    <>
                        <fieldset className="survey-fieldset">
                            <legend>Typ Wydarzenia (wybierz jedno lub więcej)</legend>
                            <div className="checkbox-group">
                                {eventTypes.map(type => (
                                    <label key={type.value} className="checkbox-label">
                                        <input type="checkbox" name="eventTypes" value={type.value} checked={formData.eventTypes.includes(type.value)} onChange={handleCheckboxChange} disabled={isLoading} />
                                        <span>{type.label}</span>
                                    </label>
                                ))}
                            </div>
                        </fieldset>
                        <div className="form-group">
                            <label htmlFor="genreTags">Preferowane Gatunki / Tagi / Słowa Kluczowe (oddziel przecinkami)</label>
                            <input type="text" id="genreTags" name="genreTags" value={formData.genreTags} onChange={handleInputChange} placeholder="np. rock alternatywny, indie, komedia..." disabled={isLoading} />
                        </div>
                    </>
                )}

                {currentStep === 2 && (
                    <div className="form-row"> {/* JSX change: wrapper for two columns */}
                        <div className="form-group form-column">
                            <label htmlFor="location">Miasto</label>
                            <input type="text" id="location" name="location" value={formData.location} onChange={handleInputChange} placeholder="np. Warszawa" disabled={isLoading} />
                        </div>
                        <fieldset className="survey-fieldset form-column">
                            <legend>Preferowany Typ Miejsca</legend>
                            <div className="checkbox-group compact-checkbox-group"> {/* Optional: more compact for columns */}
                                {venueTypes.map(type => (
                                    <label key={type.value} className="checkbox-label">
                                        <input type="checkbox" name="venueTypes" value={type.value} checked={formData.venueTypes.includes(type.value)} onChange={handleCheckboxChange} disabled={isLoading || (formData.venueTypes.includes('Dowolne') && type.value !== 'Dowolne')} />
                                        <span>{type.label}</span>
                                    </label>
                                ))}
                                <label className="checkbox-label">
                                    <input type="checkbox" name="venueTypes" value="Dowolne" checked={formData.venueTypes.includes("Dowolne")} onChange={handleCheckboxChange} disabled={isLoading} />
                                    <span>Dowolne</span>
                                </label>
                            </div>
                        </fieldset>
                    </div>
                )}

                {currentStep === 3 && (
                    <div className="form-row"> {/* JSX change: wrapper for two columns */}
                        <fieldset className="survey-fieldset form-column">
                            <legend>Preferowany Czas</legend>
                            <div className="checkbox-group compact-checkbox-group">
                                {timePreferences.map(pref => (
                                    <label key={pref.value} className="checkbox-label">
                                        <input type="checkbox" name="timePref" value={pref.value} checked={formData.timePref.includes(pref.value)} onChange={handleCheckboxChange} disabled={isLoading} />
                                        <span>{pref.label}</span>
                                    </label>
                                ))}
                            </div>
                        </fieldset>
                        <fieldset className="survey-fieldset form-column">
                            <legend>Preferencje Artystów</legend>
                            <div className="radio-group">
                                <label className="radio-label">
                                    <input type="radio" name="artistPref" value="Mniej znani" checked={formData.artistPref === 'Mniej znani'} onChange={handleRadioChange} disabled={isLoading} />
                                    <span>Raczej mniej znani / odkrycia</span>
                                </label>
                                <label className="radio-label">
                                    <input type="radio" name="artistPref" value="Znani" checked={formData.artistPref === 'Znani'} onChange={handleRadioChange} disabled={isLoading} />
                                    <span>Znani / popularni</span>
                                </label>
                                <label className="radio-label">
                                    <input type="radio" name="artistPref" value="Bez znaczenia" checked={formData.artistPref === 'Bez znaczenia'} onChange={handleRadioChange} disabled={isLoading} />
                                    <span>Bez znaczenia</span>
                                </label>
                            </div>
                        </fieldset>
                    </div>
                )}

                {currentStep === 4 && (
                    <>
                        <fieldset className="survey-fieldset">
                            <legend>Preferowana Atmosfera (opcjonalnie, wybierz max. 2)</legend>
                            <div className="checkbox-group">
                                {atmosphereOptions.map(opt => (
                                    <label key={opt.value} className="checkbox-label">
                                        <input type="checkbox" name="atmosphere" value={opt.value} checked={formData.atmosphere.includes(opt.value)} onChange={handleCheckboxChange} disabled={isLoading || (formData.atmosphere.length >= 2 && !formData.atmosphere.includes(opt.value))} />
                                        <span>{opt.label}</span>
                                    </label>
                                ))}
                            </div>
                        </fieldset>
                        <div className="form-group">
                            <label htmlFor="description">Dodatkowy opis / preferencje (opcjonalnie)</label>
                            <textarea id="description" name="description" rows="4" value={formData.description} onChange={handleInputChange} placeholder="np. szukam czegoś spokojnego, wydarzenie z dobrym jedzeniem..." disabled={isLoading}></textarea>
                        </div>
                    </>
                )}

                {/* Action Buttons */}
                <div className="survey-actions multi-step">
                    <Button type="button" variant="ghost" onClick={() => { setIsSurveyActive(false); setCurrentStep(1);}} disabled={isLoading}>
                        Anuluj
                    </Button>
                    <div className="step-navigation">
                        {currentStep > 1 && (
                            <Button type="button" variant="secondary" onClick={prevStep} disabled={isLoading}>
                                Wstecz
                            </Button>
                        )}
                        {currentStep < surveySteps.length ? (
                            <Button type="button" variant="primary" onClick={nextStep} disabled={isLoading}>
                                Dalej
                            </Button>
                        ) : (
                            <Button
                                type="button" // Changed from submit
                                variant="primary"
                                disabled={isLoading}
                                onClick={handleFinalSubmit} // Call explicit handler
                                className="find-recommendations-btn"
                            >
                                {isLoading ? 'Szukam...' : 'Znajdź Rekomendacje'}
                            </Button>
                        )}
                    </div>
                </div>
            </form>
        </div>
    );
};

export default PreferenceSurvey;
